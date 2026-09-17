package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECUtilities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEEntropyNode extends BaseChrononNodeBlockEntity {
    private static final int BASE_CONSUMPTION = 5;      // 5 ch/tick

    @Getter
    private final FluidTank fluidHandler = new FluidTank(25600, fluidStack -> fluidStack.is(TECUtilities.GAS_ENTROPY_TYPE.get()));

    public BEEntropyNode(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_NODE_BE.get(), pos, blockState, 3000);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (innerNetwork != null) {
            long consumed = innerNetwork.extractChronon(getConsumed(), false);
            if (consumed == getConsumed()) {
                fluidHandler.fill(new FluidStack(TECUtilities.GAS_ENTROPY_SOURCE.get(), 10), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public int getConsumed() {
        return BASE_CONSUMPTION;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidHandler.readFromNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidHandler.writeToNBT(registries, tag);
    }
}
