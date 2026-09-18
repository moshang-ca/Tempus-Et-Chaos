package org.moshang.tempusetchaos.blockentity.network;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.block.BlockEntropyPipe;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;
import org.moshang.tempusetchaos.registry.TECUtilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * 熵管道网络。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>整个网络共享一个大 tank（{@code stored} / {@code capacity}），
 *       所以 N 根管道只需要一次 tick，而不是 N 次 BE tick。</li>
 *   <li>拓扑不落盘：成员关系由管道方块的连接状态推导，重载时按 {@code net_uuid} 重新归位。</li>
 *   <li>存量落盘：保存时把 {@code stored} 按成员数拆成若干份写进各管道的 NBT
 *       （余数固定给坐标最小的成员，保证每根管道各自算出的份额之和严格等于 stored）。</li>
 * </ul>
 */
public class EntropyPipeNet {
    public static final int PIPE_CAPACITY = BEEntropyPipe.CAPACITY;

    /** BFS 上限，防止超长管网在拆除/卸载时卡死。 */
    public static final int MAX_NETWORK_SIZE = 4096;

    @Getter
    private final UUID uuid;

    private LongOpenHashSet members = new LongOpenHashSet();
    private long stored;
    private int capacity;
    private long minMember = Long.MAX_VALUE;

    public EntropyPipeNet(@Nullable UUID uuid) {
        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
    }

    // ---------------------------------------------------------------- 成员维护

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public long getStored() {
        return stored;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isMember(BlockPos pos) {
        return members.contains(pos.asLong());
    }

    public LongOpenHashSet memberCopy() {
        return new LongOpenHashSet(members);
    }

    public boolean join(BlockPos pos, long share) {
        long key = pos.asLong();
        if (!members.add(key)) return false;
        capacity = capacityOf(members.size());
        if (share > 0) stored += share;
        if (members.size() == 1 || key < minMember) minMember = key;
        return true;
    }

    /**
     * 成员被拆除：方块与它的 NBT 一起消失了，流体留在网络里由其余管道承接。
     */
    public void removeMember(BlockPos pos) {
        long key = pos.asLong();
        if (!members.remove(key)) return;
        capacity = capacityOf(members.size());
        if (key == minMember) recomputeMinMember();
    }

    /**
     * 成员所在区块卸载：把它已经落盘的那份份额从网络里扣掉，
     * 这样它重新加载时再加回来，总量不会多也不会少。
     */
    public void detachUnloaded(BlockPos pos, long diskShare) {
        long key = pos.asLong();
        if (!members.remove(key)) return;
        capacity = capacityOf(members.size());
        stored = Math.max(0, stored - diskShare);
        if (key == minMember) recomputeMinMember();
    }

    public void addStored(long amount) {
        if (amount != 0) stored = Math.max(0, stored + amount);
    }

    /**
     * 该成员在存档里应该拿到的份额。余数给坐标最小的成员，
     * 保证任意一根管道独立算出的结果一致，且 Σ 份额 == stored。
     */
    public long allocateShare(BlockPos pos) {
        int n = members.size();
        if (n <= 0) return 0;
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

    // ---------------------------------------------------------------- 存取接口（供管道代理 IFluidHandler 调用）

    public long fill(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted = Math.min(amount, Math.max(0, capacity - stored));
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

    // ---------------------------------------------------------------- tick

    /**
     * 与所有相邻的外部储罐（节点 / 反应堆等）做一次均分。
     * 网络自身算作一个节点参与平均，所以多余的流体会被收进管道、缺口也会被补上。
     */
    public void tick(ServerLevel level) {
        if (members.isEmpty()) return;

        List<IFluidHandler> handlers = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        for (long key : members) {
            BlockPos pos = BlockPos.of(key);
            if (!level.isLoaded(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof BEEntropyPipe pipe)) continue;
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (!level.isLoaded(neighborPos)) continue;
                // 管道之间不算端点，只有真正的外部容器才参与均分
                if (level.getBlockEntity(neighborPos) instanceof BEEntropyPipe) continue;
                if (!seen.add(neighborPos.asLong())) continue;
                IFluidHandler handler = pipe.getNeighborHandler(dir);
                if (handler != null) handlers.add(handler);
            }
        }
        if (handlers.isEmpty()) return;

        long total = stored;
        long count = 1;
        for (IFluidHandler handler : handlers) {
            total += gasAmount(handler);
            count++;
        }
        long average = total / count;

        long before = stored;
        // 高于平均的外部容器：抽回网络。抽回量受网络剩余空间限制，宁可下个 tick 再继续，也不能让存量越过 capacity。
        for (IFluidHandler handler : handlers) {
            long space = capacity - stored;
            if (space <= 0) break;
            long amount = gasAmount(handler);
            if (amount <= average) continue;
            long wanted = Math.min(amount - average, space);
            if (wanted <= 0) continue;
            FluidStack drained = handler.drain(gasStack(wanted), IFluidHandler.FluidAction.EXECUTE);
            stored += drained.getAmount();
        }
        // 低于平均的外部容器：从网络补足
        for (IFluidHandler handler : handlers) {
            long amount = gasAmount(handler);
            if (amount >= average) continue;
            int filled = handler.fill(gasStack(average - amount), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) stored = Math.max(0, stored - filled);
        }

        if (stored != before) markDirty(level);
    }

    /** 存量变化后把每个成员的区块标脏，否则区块卸载时不会先存档。 */
    public void markDirty(ServerLevel level) {
        for (long key : members) {
            BlockPos pos = BlockPos.of(key);
            if (!level.isLoaded(pos)) continue;
            if (level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) pipe.setChanged();
        }
    }

    private static long gasAmount(IFluidHandler handler) {
        long total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (!stack.isEmpty() && stack.is(TECUtilities.GAS_ENTROPY_TYPE.get())) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private static int toInt(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    // ---------------------------------------------------------------- 拓扑推导

    public static boolean isPipe(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BEEntropyPipe;
    }

    /**
     * 从 start 沿管道连接扩散，收集所在连通分量的所有管道坐标。
     *
     * @param excluded 需要绕过的坐标（通常是刚被挖掉的那根管道）
     */
    public static LongOpenHashSet collectComponent(Level level, BlockPos start, long excluded, int limit) {
        LongOpenHashSet component = new LongOpenHashSet();
        if (!isPipe(level, start)) return component;

        LongOpenHashSet visited = new LongOpenHashSet();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.asLong());
        queue.add(start);

        while (!queue.isEmpty() && component.size() < limit) {
            BlockPos current = queue.poll();
            if (!isPipe(level, current)) continue;
            component.add(current.asLong());

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                long key = next.asLong();
                if (key == excluded) continue;
                if (!visited.add(key)) continue;
                if (!isPipe(level, next)) continue;
                if (!BlockEntropyPipe.isConnected(level.getBlockState(current), dir)) continue;
                queue.add(next);
            }
        }
        return component;
    }

    /**
     * 查找与 origin 相邻（隔着若干管道也算）的已有网络。
     */
    public static Set<UUID> findAdjacentNetworks(Level level, BlockPos origin, PipeNetManager manager) {
        Set<UUID> found = new HashSet<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin.asLong());

        for (Direction dir : Direction.values()) {
            BlockPos next = origin.relative(dir);
            if (isPipe(level, next) && visited.add(next.asLong())) queue.add(next);
        }

        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            BlockPos current = queue.poll();
            if (level.getBlockEntity(current) instanceof BEEntropyPipe pipe) {
                UUID id = pipe.getNetUuid();
                if (id != null && manager.getNetwork(id) != null) found.add(id);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!visited.add(next.asLong())) continue;
                if (!isPipe(level, next)) continue;
                if (!BlockEntropyPipe.isConnected(level.getBlockState(current), dir)) continue;
                queue.add(next);
            }
        }
        return found;
    }
}
