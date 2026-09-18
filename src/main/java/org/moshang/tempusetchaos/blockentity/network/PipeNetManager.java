package org.moshang.tempusetchaos.blockentity.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PipeNetManager {
    private static final Map<ServerLevel, PipeNetManager> MANAGERS = new WeakHashMap<>();

    private static final Set<BEEntropyPipe> PENDING =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static PipeNetManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, PipeNetManager::new);
    }

    public static void enqueue(BEEntropyPipe pipe) {
        PENDING.add(pipe);
    }

    @Getter
    private final ServerLevel level;
    private final Map<UUID, EntropyPipeNet> networks = new HashMap<>();

    private PipeNetManager(ServerLevel level) {
        this.level = level;
    }

    @Nullable
    public EntropyPipeNet getNetwork(@Nullable UUID uuid) {
        return uuid == null ? null : networks.get(uuid);
    }

    public void tick() {
        drainPending();
        if (networks.isEmpty()) return;
        for (EntropyPipeNet network : new ArrayList<>(networks.values())) {
            network.tick(level);
        }
        networks.values().removeIf(EntropyPipeNet::isEmpty);
    }

    private void drainPending() {
        if (PENDING.isEmpty()) return;
        for (BEEntropyPipe pipe : new ArrayList<>(PENDING)) {
            Level pipeLevel = pipe.getLevel();
            if (pipeLevel == null) continue;
            if (pipeLevel.isClientSide) {
                PENDING.remove(pipe);
                continue;
            }
            if (pipeLevel != level) continue;
            PENDING.remove(pipe);
            attach(pipe);
        }
    }

    private void attach(BEEntropyPipe pipe) {
        BlockPos pos = pipe.getBlockPos();
        if (pipe.isRemoved()) return;

        long share = pipe.consumePendingShare();
        UUID pendingId = pipe.getNetUuid();
        EntropyPipeNet network = pendingId == null ? null : networks.get(pendingId);
        Set<UUID> adjacent = network != null
                ? Collections.emptySet()
                : EntropyPipeNet.findAdjacentNetworks(level, pos, this);

        if (network == null) {
            if (adjacent.isEmpty()) {
                network = new EntropyPipeNet(level, pendingId);
                networks.put(network.getUuid(), network);
            } else {
                Iterator<UUID> iterator = adjacent.iterator();
                UUID primaryId = iterator.next();
                network = networks.get(primaryId);
                if (network == null) {
                    network = new EntropyPipeNet(level, primaryId);
                    networks.put(primaryId, network);
                }
                while (iterator.hasNext()) {
                    mergeInto(network, networks.get(iterator.next()));
                }
            }
        } else {
            for (UUID otherId : adjacent) {
                if (otherId.equals(network.getUuid())) continue;
                mergeInto(network, networks.get(otherId));
            }
        }

        network.join(pos, share);
        if (!network.getUuid().equals(pipe.getNetUuid())) {
            pipe.setNetUuid(network.getUuid());
            pipe.setChanged();
        }
    }

    public void onPipeBroken(BlockPos pos) {
        EntropyPipeNet network = findNetworkContaining(pos);
        if (network == null) return;
        network.removeMember(pos);
        splitIfNeeded(pos, network);
    }

    public void onPipeUnloaded(BEEntropyPipe pipe) {
        BlockPos pos = pipe.getBlockPos();
        EntropyPipeNet network = getNetwork(pipe.getNetUuid());
        if (network == null) {
            network = findNetworkContaining(pos);
            if (network == null) return;
        }
        long share = pipe.hasSavedShare() ? pipe.getSavedShare() : network.allocateShare(pos);
        network.detachUnloaded(pos, share);
        splitIfNeeded(pos, network);
    }

    @Nullable
    private EntropyPipeNet findNetworkContaining(BlockPos pos) {
        for (EntropyPipeNet network : networks.values()) {
            if (network.isMember(pos)) return network;
        }
        return null;
    }

    private void mergeInto(EntropyPipeNet primary, @Nullable EntropyPipeNet other) {
        if (other == null || other == primary) return;

        long moved = other.getStored();
        for (long key : other.memberCopy()) {
            BlockPos pos = BlockPos.of(key);
            primary.join(pos, 0);
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
                pipe.setNetUuid(primary.getUuid());
                pipe.setChanged();
            }
        }
        primary.addStored(moved);
        networks.remove(other.getUuid());
    }

    private void splitIfNeeded(BlockPos removedPos, EntropyPipeNet network) {
        if (network.size() <= 1) return;
        if (countMemberNeighbors(network, removedPos) <= 1) return;

        List<LongOpenHashSet> components = network.memberComponents();
        if (components.size() <= 1) return;
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));

        long keptStored = network.getStored();
        networks.remove(network.getUuid());

        for (int i = 0; i < components.size(); i++) {
            EntropyPipeNet split = new EntropyPipeNet(level, null);
            for (long key : components.get(i)) {
                BlockPos pos = BlockPos.of(key);
                split.join(pos, 0);
                if (!level.isLoaded(pos)) continue;
                if (level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
                    pipe.setNetUuid(split.getUuid());
                    pipe.setChanged();
                }
            }
            if (split.isEmpty()) continue;
            if (i == 0) split.addStored(keptStored);
            networks.put(split.getUuid(), split);
        }
    }

    private static int countMemberNeighbors(EntropyPipeNet network, BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (network.isMember(pos.relative(dir))) count++;
        }
        return count;
    }
}
