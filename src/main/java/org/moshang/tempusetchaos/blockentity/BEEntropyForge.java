package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IValueChannel;
import org.moshang.tempusetchaos.api.RecipeableChrononNodeBE;
import org.moshang.tempusetchaos.api.storage.IO;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BEEntropyForge extends RecipeableChrononNodeBE<ProcessingRecipe.EntropyForging> {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOTS = 3;

    @Getter
    private final FluidTank entropyTank = new FluidTank(50000, stack -> stack.is(TECFluids.GAS_ENTROPY_TYPE.get())) {
        @Override
        protected void onContentsChanged() {
            recipeDirty = true;
            setChanged();
        }
    };
    @Getter
    private final IEnergyStorage energyStorage = new EnergyStorage(120000);
    @Getter
    private final NotifiedItemStackHandler inputHandler = new NotifiedItemStackHandler(INPUT_SLOTS, IO.INPUT)
            .setOnContentsChanged(() -> { recipeDirty = true; setChanged(); });
    @Getter
    private final NotifiedItemStackHandler outputHandler = new NotifiedItemStackHandler(OUTPUT_SLOTS, IO.OUTPUT)
            .setOnContentsChanged(this::setChanged);

    @Getter(lazy = true)
    private final CombinedInvWrapper invWrapper = new CombinedInvWrapper(inputHandler, outputHandler);

    @Nullable
    private IValueChannel cachedEnergy;

    public BEEntropyForge(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_FORGE_BE.get(), pos, blockState,
                24000, TECRecipes.ENTROPY_FORGE_RT.get());
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    protected List<NotifiedItemStackHandler> itemInputs() {
        return List.of(inputHandler);
    }

    @Override
    protected List<NotifiedItemStackHandler> itemOutputs() {
        return List.of(outputHandler);
    }

    @Override
    protected List<IFluidHandler> fluidInputs() {
        return List.of(entropyTank);
    }

    @Override
    protected IValueChannel energyChannel() {
        if (cachedEnergy == null) cachedEnergy = new EnergyChannel(energyStorage);
        return cachedEnergy;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        entropyTank.readFromNBT(registries, tag);
        if (tag.contains("inputs")) inputHandler.deserializeNBT(registries, tag.getCompound("inputs"));
        if (tag.contains("outputs")) outputHandler.deserializeNBT(registries, tag.getCompound("outputs"));
        if (tag.contains("energy")) energyStorage.receiveEnergy(tag.getInt("energy"), false);
        recipeDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        entropyTank.writeToNBT(registries, tag);
        tag.put("inputs", inputHandler.serializeNBT(registries));
        tag.put("outputs", outputHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
    }
}
