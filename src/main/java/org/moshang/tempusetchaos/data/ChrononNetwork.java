package org.moshang.tempusetchaos.data;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IChrononNode;
import org.moshang.tempusetchaos.api.IChrononStorage;
import org.moshang.tempusetchaos.block.BlockChrononNetCable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChrononNetwork implements IChrononStorage {
    public static final Map<UUID, ChrononNetwork> NETWORK = new ConcurrentHashMap<>();

    @Nullable
    public static ChrononNetwork get(UUID uuid) { return NETWORK.get(uuid); }

    @Getter
    private final UUID uuid;
    @Getter
    private long chrononStored;
    @Getter
    private long capacity;
    private final WeakReference<Level> levelRef;
    private final Set<BlockPos> energySources = new HashSet<>();
    private final Set<BlockPos> energySinks = new HashSet<>();
    private final Set<BlockPos> energyStorages = new HashSet<>();

    public ChrononNetwork(Level level) {
        this(UUID.randomUUID(), level);
    }

    public ChrononNetwork(UUID uuid, Level level) {
        this.uuid = uuid;
        this.levelRef = new WeakReference<>(level);
        NETWORK.put(uuid, this);
    }

    public Set<BlockPos> getConnectors() {
        int capacity = (int) (energySinks.size() + energySources.size() + energyStorages.size() / .75f) + 1;
        Set<BlockPos> connectors = new HashSet<>(capacity);
        connectors.addAll(energySources);
        connectors.addAll(energySinks);
        connectors.addAll(energyStorages);
        return connectors;
    }

    public void addNode(IChrononNode node) {
        IChrononNode.NodeType type = node.getNodeType();
        switch (node.getNodeType()) {
            case PRODUCER -> energySources.add(node.getNodePos());
            case CONSUMER -> energySinks.add(node.getNodePos());
            case STORAGE -> energyStorages.add(node.getNodePos());
        }
        this.capacity += node.getCapacity();
        markDirty();
    }

    public void removeNode(IChrononNode node) {
        switch (node.getNodeType()) {
            case PRODUCER -> energySources.remove(node.getNodePos());
            case CONSUMER -> energySinks.remove(node.getNodePos());
            case STORAGE -> energyStorages.remove(node.getNodePos());
        }
        this.capacity -= node.getCapacity();
        markDirty();
    }

    public void merge(UUID networkId) {
        if (networkId.equals(uuid)) return;
        ChrononNetwork other = NETWORK.get(networkId);
        if (other == null) return;

        this.capacity += other.capacity;
        this.chrononStored += other.chrononStored;
        for (BlockPos nodePos : other.getConnectors()) {
            IChrononNode node = getNodeAt(nodePos);
            if (node == null) continue;
            addNode(node);
            node.setNetworkUUID(this.uuid);
        }
        NETWORK.remove(networkId);
    }

    public List<ChrononNetwork> checkAndSplit() {
        Set<BlockPos> remaining = getConnectors();

        if(remaining.isEmpty()) {
            NETWORK.remove(uuid);
            return Collections.emptyList();
        }

        List<Set<BlockPos>> components = findConnectedComponent(remaining);
        if (components.size() <= 1) return Collections.emptyList();

        return splitNetwork(components);
    }

    public boolean isEmpty() {
        return energySinks.size() + energyStorages.size() + energySources.size() == 0;
    }

    public void markDirty() {
        Level level = this.levelRef.get();
        if (level instanceof ServerLevel serverLevel)
            ChrononNetworkData.get(serverLevel).save();
    }

    private List<ChrononNetwork> splitNetwork(List<Set<BlockPos>> components) {
        List<ChrononNetwork> newNetworks = new ArrayList<>();

        for (var component : components) {
            Level level = levelRef.get();
            if (level == null) break;
            ChrononNetwork network = new ChrononNetwork(levelRef.get());
            for (BlockPos pos : component) {
                IChrononNode node = level.getBlockEntity(pos) instanceof IChrononNode n ? n : null;
                if (node != null) {
                    network.addNode(node);
                    node.setNetworkUUID(network.uuid);
                }
            }
            if (!network.isEmpty()) {
                NETWORK.put(network.uuid, network);
                newNetworks.add(network);
            }
        }
        NETWORK.remove(this.uuid);
        return newNetworks;
    }

    private List<Set<BlockPos>> findConnectedComponent(Set<BlockPos> nodes) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (BlockPos start : nodes) {
            if (visited.contains(start)) continue;

            Set<BlockPos> component = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                if (nodes.contains(current)) component.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (visited.contains(neighbor)) continue;

                    if (nodes.contains(neighbor) || isCable(this.levelRef.get(), neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            if (!component.isEmpty()) {
                components.add(component);
            }
        }
        return components;
    }

    @Nullable
    private IChrononNode getNodeAt(BlockPos pos) {
        Level level = levelRef.get();
        if (level == null) return null;
        return level.getBlockEntity(pos) instanceof IChrononNode node ? node : null;
    }

    @Override
    public long receiveChronon(long amount, boolean simulate) {
        if (!canReceive() || amount <= 0) return 0;
        long beReceived = Mth.clamp(capacity - chrononStored, 0, amount);

        if (!simulate) {
            chrononStored += beReceived;
            markDirty();
        }
        return beReceived;
    }

    @Override
    public long extractChronon(long amount, boolean simulate) {
        if (!canExtract() || amount <= 0) return 0;

        long beExtracted = Math.min(chrononStored, amount);
        if (!simulate) {
            chrononStored -= beExtracted;
            markDirty();
        }
        return beExtracted;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        tag.putLong("chronon_stored", chrononStored);
        tag.putLong("chronon_capacity", capacity);

        tag.put("sources", serializeSet(energySources));
        tag.put("sinks", serializeSet(energySinks));
        tag.put("storages", serializeSet(energyStorages));
        return tag;
    }

    @Nullable
    @SuppressWarnings("UnusedReturnValue")
    public static ChrononNetwork deserialize(CompoundTag tag, ServerLevel level) {
        if (!tag.hasUUID("uuid")) return null;

        UUID uuid = tag.getUUID("uuid");
        ChrononNetwork network = new ChrononNetwork(uuid, level);
        network.chrononStored = tag.getLong("chronon_stored");
        network.capacity = tag.getLong("chronon_capacity");

        network.energySources.addAll(deserializeSet(tag.getList("sources", Tag.TAG_COMPOUND)));
        network.energySinks.addAll(deserializeSet(tag.getList("sinks", Tag.TAG_COMPOUND)));
        network.energyStorages.addAll(deserializeSet(tag.getList("storages", Tag.TAG_COMPOUND)));

        return network;
    }

    public static Set<UUID> findAdjacent(Level level, BlockPos pos) {
        Set<UUID> found = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (Direction dir : Direction.values()) {
            queue.add(pos.relative(dir));
        }

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (level.getBlockEntity(current) instanceof IChrononNode node) {
                UUID foundNetworkUUID = node.getNetworkUUID();
                if (foundNetworkUUID != null && NETWORK.containsKey(foundNetworkUUID))
                    found.add(foundNetworkUUID);
                continue;
            }

            if (isCable(level, current)) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir);
                    if (!visited.contains(next))
                        queue.add(next);
                }
            }
        }
        return found;
    }

    private static boolean isCable(Level level, BlockPos pos) {
        if (level == null) return false;
        return level.getBlockState(pos).getBlock() instanceof BlockChrononNetCable;
    }

    public static ListTag serializeSet(Set<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            list.add(NbtUtils.writeBlockPos(pos));
        }
        return list;
    }

    public static Set<BlockPos> deserializeSet(ListTag tag) {
        Set<BlockPos> positions = new HashSet<>();
        for (int i = 0; i < tag.size(); ++i) {
            int[] posInt = tag.getIntArray(i);
            positions.add(new BlockPos(posInt[0], posInt[1], posInt[2]));
        }
        return positions;
    }
}
