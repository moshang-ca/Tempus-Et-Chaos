package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IEntropyPipeConnectable;
import org.moshang.tempusetchaos.blockentity.network.EntropyPipeNet;
import org.moshang.tempusetchaos.blockentity.network.PipeNetManager;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECCapabilities;
import org.moshang.tempusetchaos.registry.TECUtilities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class BEEntropyPipe extends BlockEntity implements IEntropyPipeConnectable {
    public static final int CAPACITY = 30_000;

    private static final String NET_UUID_KEY = "net_uuid";
    private static final String SHARE_KEY = "entropy_share";

    @Getter
    @Setter
    private UUID netUuid;

    @Getter
    private long savedShare;
    private boolean saved;

    private long pendingShare;

    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IFluidHandler, Direction>[] handlerCaches = new BlockCapabilityCache[6];

    @Getter
    private final IFluidHandler fluidHandler = new NetFluidHandler();

    public BEEntropyPipe(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_PIPE_BE.get(), pos, blockState);
    }

    public boolean hasSavedShare() {
        return saved;
    }

    public long consumePendingShare() {
        long share = pendingShare;
        pendingShare = 0;
        return share;
    }

    @Nullable
    public EntropyPipeNet getNet() {
        if (netUuid == null || !(level instanceof ServerLevel serverLevel)) return null;
        return PipeNetManager.get(serverLevel).getNetwork(netUuid);
    }

    @Nullable
    public IFluidHandler getNeighborHandler(Direction dir) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        int index = dir.ordinal();
        BlockCapabilityCache<IFluidHandler, Direction> cache = handlerCaches[index];
        if (cache == null) {
            cache = BlockCapabilityCache.create(
                    TECCapabilities.FLUID_ENTROPY,
                    serverLevel,
                    getBlockPos().relative(dir),
                    dir.getOpposite(),
                    () -> !this.isRemoved(),
                    () -> {
                    }
            );
            handlerCaches[index] = cache;
        }
        return cache.getCapability();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Arrays.fill(handlerCaches, null);
        if (level instanceof ServerLevel serverLevel) {
            PipeNetManager.get(serverLevel).onPipeUnloaded(this);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(NET_UUID_KEY)) {
            this.netUuid = tag.getUUID(NET_UUID_KEY);
        }
        if (tag.contains(SHARE_KEY)) {
            this.savedShare = tag.getLong(SHARE_KEY);
            this.pendingShare = this.savedShare;
            this.saved = true;
        }
        PipeNetManager.enqueue(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (netUuid != null) {
            tag.putUUID(NET_UUID_KEY, netUuid);
        }
        EntropyPipeNet network = getNet();
        if (network != null && network.isMember(getBlockPos())) {
            this.savedShare = network.allocateShare(getBlockPos());
            this.saved = true;
        }
        tag.putLong(SHARE_KEY, this.savedShare);
    }

    @ParametersAreNonnullByDefault
    private final class NetFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        @NotNull
        public FluidStack getFluidInTank(int tank) {
            EntropyPipeNet network = getNet();
            return network == null ? FluidStack.EMPTY : EntropyPipeNet.gasStack(network.getStored());
        }

        @Override
        public int getTankCapacity(int tank) {
            EntropyPipeNet network = getNet();
            return network == null ? 0 : network.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(TECUtilities.GAS_ENTROPY_TYPE.get());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            EntropyPipeNet network = getNet();
            if (network == null) return 0;
            long accepted = network.fill(resource.getAmount(), action.simulate());
            if (action.execute() && accepted > 0) markDirty(network);
            return (int) accepted;
        }

        @Override
        @NotNull
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        @NotNull
        public FluidStack drain(int maxDrain, FluidAction action) {
            EntropyPipeNet network = getNet();
            if (network == null) return FluidStack.EMPTY;
            long drained = network.drain(maxDrain, action.simulate());
            if (drained <= 0) return FluidStack.EMPTY;
            if (action.execute()) markDirty(network);
            return EntropyPipeNet.gasStack(drained);
        }

        private void markDirty(EntropyPipeNet network) {
            if (level instanceof ServerLevel serverLevel) network.markDirty(serverLevel);
        }
    }
}
