package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.api.EntropyModifiers;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEEntropyNode extends BaseChrononNodeBlockEntity {
    private static final int BASE_CONSUMPTION = 5;      // 5 ch/tick
    private static final int BASE_GAS = 10;             // 10 mB/tick

    @Getter
    private final FluidTank fluidTank = new FluidTank(25600, fluidStack -> fluidStack.is(TECFluids.GAS_ENTROPY_TYPE.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    @Setter
    private EntropyModifiers modifiers = EntropyModifiers.NONE;

    public BEEntropyNode(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_NODE_BE.get(), pos, blockState, 3000);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (innerNetwork == null) return;
        int gas = gasPerTick();
        if (gas <= 0) return;
        if (innerNetwork.extractChronon(consumed(), true) < consumed()) return;
        FluidStack output = new FluidStack(TECFluids.GAS_ENTROPY_SOURCE.get(), gas);
        if (fluidTank.fill(output, IFluidHandler.FluidAction.SIMULATE) < gas) return;
        innerNetwork.extractChronon(consumed(), false);
        fluidTank.fill(output, IFluidHandler.FluidAction.EXECUTE);
    }

    private int consumed() {
        return Math.max(0, Math.round(BASE_CONSUMPTION * modifiers.chrononCost()));
    }

    private int gasPerTick() {
        return Math.max(0, Math.round(BASE_GAS * modifiers.gasRate()));
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public int getConsumed() {
        return consumed();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fluidTank.writeToNBT(registries, tag);
    }
}
