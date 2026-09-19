package org.moshang.tempusetchaos.blockentity.network;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.EntropyPipeFaceMode;
import org.moshang.tempusetchaos.block.BlockEntropyPipe;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;
import org.moshang.tempusetchaos.registry.TECUtilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntropyPipeNet {
    public static final int PIPE_CAPACITY = BEEntropyPipe.CAPACITY;

    public static final long BASE_RATE = 1_000L;
    public static final long MAX_RATE = 2_000L;
    private static final double TICKS_PER_SECOND = 20.0D;

    @Getter
    private final UUID uuid;

    private final LongOpenHashSet members = new LongOpenHashSet();
    @Getter
    private long stored;
    @Getter
    private int capacity;
    private long minMember = Long.MAX_VALUE;
    private int extractCursor;
    private int outputCursor;

    private final List<Endpoint> extractEndpoints = new ArrayList<>();
    private final List<Endpoint> outputEndpoints = new ArrayList<>();
    private final List<Endpoint> tickExtract = new ArrayList<>();
    private final List<Endpoint> tickOutput = new ArrayList<>();
    private double cachedMultiplier = BEEntropyPipe.BASE_THROUGHPUT_MULTIPLIER;
    private final LongOpenHashSet dirtyChunks = new LongOpenHashSet();
    private final Set<PendingFace> pendingFaces = new LinkedHashSet<>();

    @Getter
    private final ServerLevel level;

    public EntropyPipeNet(ServerLevel level, @Nullable UUID uuid) {
        this.level = level;
        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public boolean isMember(BlockPos pos) {
        return members.contains(pos.asLong());
    }

    public LongOpenHashSet memberCopy() {
        return new LongOpenHashSet(members);
    }

    @CanIgnoreReturnValue
    public boolean join(BlockPos pos, long share) {
        long key = pos.asLong();
        if (!members.add(key)) return false;
        capacity = capacityOf(members.size());
        if (share > 0) stored += share;
        if (members.size() == 1 || key < minMember) minMember = key;
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
            if (pipe.getThroughputMultiplier() > cachedMultiplier) {
                cachedMultiplier = pipe.getThroughputMultiplier();
            }
            for (Direction dir : Direction.values()) {
                addEndpoint(pipe, dir, pipe.getFaceMode(dir));
            }
        }
        return true;
    }

    public void removeMember(BlockPos pos) {
        long key = pos.asLong();
        if (!members.remove(key)) return;
        capacity = capacityOf(members.size());
        if (key == minMember) recomputeMinMember();
        removeEndpointsForOwner(key);
    }

    public void detachUnloaded(BlockPos pos, long diskShare) {
        long key = pos.asLong();
        if (!members.remove(key)) return;
        capacity = capacityOf(members.size());
        stored = Math.max(0, stored - diskShare);
        if (key == minMember) recomputeMinMember();
        removeEndpointsForOwner(key);
    }

    public void invalidateFace(BEEntropyPipe pipe, Direction dir) {
        pendingFaces.add(new PendingFace(pipe, dir));
    }

    private void applyPendingFaces() {
        if (pendingFaces.isEmpty()) return;
        List<PendingFace> batch = new ArrayList<>(pendingFaces);
        pendingFaces.clear();
        for (PendingFace face : batch) {
            refreshEndpoint(face.pipe(), face.dir());
        }
    }

    private void refreshEndpoint(BEEntropyPipe pipe, Direction dir) {
        long ownerKey = pipe.getBlockPos().asLong();
        if (!members.contains(ownerKey)) return;
        extractEndpoints.removeIf(e -> e.ownerKey() == ownerKey && e.dir() == dir);
        outputEndpoints.removeIf(e -> e.ownerKey() == ownerKey && e.dir() == dir);
        addEndpoint(pipe, dir, pipe.getFaceMode(dir));
    }

    private void addEndpoint(BEEntropyPipe pipe, Direction dir, EntropyPipeFaceMode mode) {
        if (mode == EntropyPipeFaceMode.NONE) return;
        if (pipe.getNeighborHandler(dir) == null) return;
        Level pipeLevel = pipe.getLevel();
        if (pipeLevel != null && pipeLevel.getBlockEntity(pipe.getBlockPos().relative(dir)) instanceof BEEntropyPipe) {
            return;
        }
        (mode == EntropyPipeFaceMode.EXTRACT ? extractEndpoints : outputEndpoints)
                .add(new Endpoint(pipe, dir, pipe.getBlockPos().asLong()));
    }

    private void removeEndpointsForOwner(long ownerKey) {
        extractEndpoints.removeIf(e -> e.ownerKey() == ownerKey);
        outputEndpoints.removeIf(e -> e.ownerKey() == ownerKey);
    }

    public void addStored(long amount) {
        if (amount != 0) stored = Math.max(0, stored + amount);
    }

    public long allocateShare(BlockPos pos) {
        int n = members.size();
        if (n == 0) return 0;
        long base = stored / n;
        long remainder = stored - base * n;
        return pos.asLong() == minMember ? base + remainder : base;
    }

    private void recomputeMinMember() {
        long min = Long.MAX_VALUE;
        for (long key : members) {
            if (key < min) min = key;
        }
        minMember = min;
    }

    private static int capacityOf(int memberCount) {
        return (int) Math.min((long) memberCount * PIPE_CAPACITY, Integer.MAX_VALUE);
    }

    public long fill(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted = Math.clamp(capacity - stored, 0, amount);
        if (!simulate && accepted > 0) stored += accepted;
        return accepted;
    }

    public long drain(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long drained = Math.min(amount, stored);
        if (!simulate && drained > 0) stored -= drained;
        return drained;
    }

    public static FluidStack gasStack(long amount) {
        if (amount <= 0) return FluidStack.EMPTY;
        return new FluidStack(TECUtilities.GAS_ENTROPY_SOURCE.get(), toInt(amount));
    }

    public void tick(ServerLevel level) {
        if (members.isEmpty()) return;
        applyPendingFaces();

        tickExtract.clear();
        tickExtract.addAll(extractEndpoints);
        tickOutput.clear();
        tickOutput.addAll(outputEndpoints);

        if (tickExtract.isEmpty()) return;

        long rate = ratePerTick(cachedMultiplier);
        long before = stored;
        long outputBudget = rate;

        if (stored > 0) {
            long accepted = fillOutputs(tickOutput, Math.min(stored, outputBudget));
            stored -= accepted;
            outputBudget -= accepted;
        }

        long extractBudget = rate;
        int size = tickExtract.size();
        for (int i = 0; i < size && extractBudget > 0; i++) {
            long space = capacity - stored;
            if (space <= 0) break;
            IFluidHandler source = tickExtract.get(Math.floorMod(extractCursor + i, size)).handler();
            if (source == null) continue;
            long amount = Math.min(extractBudget, space);
            FluidStack drained = source.drain(gasStack(amount), IFluidHandler.FluidAction.EXECUTE);
            long got = drained.getAmount();
            if (got <= 0) continue;
            extractBudget -= got;

            long accepted = outputBudget > 0 ? fillOutputs(tickOutput, Math.min(got, outputBudget)) : 0;
            outputBudget -= accepted;
            stored += got - accepted;
        }
        extractCursor = (extractCursor + 1) % size;

        if (stored != before) markDirty(level);
    }

    private long ratePerTick(double multiplier) {
        double perSecond = Math.min(MAX_RATE,
                BASE_RATE * Math.max(BEEntropyPipe.BASE_THROUGHPUT_MULTIPLIER, multiplier));
        return Math.max(1L, (long) (perSecond / TICKS_PER_SECOND));
    }

    private long fillOutputs(List<Endpoint> outputs, long amount) {
        if (amount <= 0 || outputs.isEmpty()) return 0;
        int size = outputs.size();
        long filled = 0;
        for (int i = 0; i < size && filled < amount; i++) {
            IFluidHandler handler = outputs.get(Math.floorMod(outputCursor + i, size)).handler();
            if (handler == null) continue;
            int accepted = handler.fill(gasStack(amount - filled), IFluidHandler.FluidAction.EXECUTE);
            if (accepted <= 0) continue;
            filled += accepted;
        }
        outputCursor = (outputCursor + 1) % size;
        return filled;
    }

    public void markDirty(ServerLevel level) {
        dirtyChunks.clear();
        for (long key : members) {
            BlockPos pos = BlockPos.of(key);
            if (!level.isLoaded(pos)) continue;
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (dirtyChunks.contains(chunkKey)) continue;
            if (level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
                pipe.setChanged();
                dirtyChunks.add(chunkKey);
            }
        }
    }

    private static int toInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    public List<LongOpenHashSet> memberComponents() {
        List<LongOpenHashSet> components = new ArrayList<>();
        LongOpenHashSet assigned = new LongOpenHashSet();
        LongArrayList stack = new LongArrayList();

        for (long seed : members) {
            if (!assigned.add(seed)) continue;

            LongOpenHashSet component = new LongOpenHashSet();
            component.add(seed);
            stack.clear();
            stack.add(seed);

            while (!stack.isEmpty()) {
                BlockPos current = BlockPos.of(stack.removeLong(stack.size() - 1));
                for (Direction dir : Direction.values()) {
                    long next = current.relative(dir).asLong();
                    if (!members.contains(next)) continue;
                    if (!assigned.add(next)) continue;
                    component.add(next);
                    stack.add(next);
                }
            }
            components.add(component);
        }
        return components;
    }

    public static Set<UUID> findAdjacentNetworks(Level level, BlockPos origin, PipeNetManager manager) {
        Set<UUID> found = new HashSet<>();
        BlockState originState = level.getBlockState(origin);
        for (Direction dir : Direction.values()) {
            if (!BlockEntropyPipe.isConnected(originState, dir)) continue;

            BlockPos neighborPos = origin.relative(dir);
            if (!(level.getBlockEntity(neighborPos) instanceof BEEntropyPipe pipe)) continue;
            UUID id = pipe.getNetUuid();
            if (id != null && manager.getNetwork(id) != null) found.add(id);
        }
        return found;
    }

    private record PendingFace(BEEntropyPipe pipe, Direction dir) { }

    private record Endpoint(BEEntropyPipe pipe, Direction dir, long ownerKey) {
        @Nullable
        IFluidHandler handler() {
            return pipe.getNeighborHandler(dir);
        }
    }
}
