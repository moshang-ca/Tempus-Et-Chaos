package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.moshang.tempusetchaos.api.RecipeableChrononNodeBE;
import org.moshang.tempusetchaos.api.storage.IO;
import org.moshang.tempusetchaos.api.storage.NotifiedItemStackHandler;
import org.moshang.tempusetchaos.recipe.EntropyForgeRecipe;
import org.moshang.tempusetchaos.recipe.input.UniversalRecipeInput;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BEEntropyForge extends RecipeableChrononNodeBE<UniversalRecipeInput, EntropyForgeRecipe> {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOT = 0;

    private static final long CHRONON_QUERY = Integer.MAX_VALUE;

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
    private final NotifiedItemStackHandler outputHandler = new NotifiedItemStackHandler(1, IO.OUTPUT)
            .setOnContentsChanged(this::setChanged);

    @Getter(lazy = true)
    private final CombinedInvWrapper invWrapper = new CombinedInvWrapper(inputHandler, outputHandler);

    public BEEntropyForge(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_FORGE_BE.get(), pos, blockState,
                24000, TECRecipes.ENTROPY_FORGE_RT.get());
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    protected List<UniversalRecipeInput> buildInputs() {
        List<ItemStack> items = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            items.add(inputHandler.getStackInSlot(i));
        }
        List<FluidStack> fluids = List.of(entropyTank.getFluid());
        int chrononInput = (int) Math.min(Integer.MAX_VALUE, availableChronon());
        return List.of(new UniversalRecipeInput(items, fluids, chrononInput, energyStorage.getEnergyStored()));
    }

    @Override
    protected boolean onRecipeStart(EntropyForgeRecipe recipe) {
        if (energyStorage.getEnergyStored() < recipe.getEnergyCost()) return false;
        if (recipe.getChrononCost() > 0 && availableChronon() < recipe.getChrononCost()) return false;
        if (!consume(recipe.getItemInputs(), inputHandler)) return false;

        int fluidCost = recipe.getFluidInputs().stream().mapToInt(FluidStack::getAmount).sum();
        if (fluidCost > 0) entropyTank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        if (recipe.getChrononCost() > 0) innerNetwork.extractChronon(recipe.getChrononCost(), false);
        if (recipe.getEnergyCost() > 0) energyStorage.extractEnergy(recipe.getEnergyCost(), false);
        return true;
    }

    @Override
    protected boolean produce(EntropyForgeRecipe recipe, boolean finishing) {
        assert level != null;
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!canAccept(result)) return false;
        outputHandler.insertItem(OUTPUT_SLOT, result, false);
        return true;
    }

    private long availableChronon() {
        return innerNetwork == null ? 0 : innerNetwork.extractChronon(CHRONON_QUERY, true);
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = outputHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        entropyTank.readFromNBT(registries, tag);
        if (tag.contains("inputs")) inputHandler.deserializeNBT(registries, tag.getCompound("inputs"));
        if (tag.contains("fluids")) entropyTank.readFromNBT(registries, tag.getCompound("fluids"));
        if (tag.contains("outputs")) outputHandler.deserializeNBT(registries, tag.getCompound("outputs"));
        if (tag.contains("energy")) energyStorage.receiveEnergy(tag.getInt("energy"), false);
        recipeDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        entropyTank.writeToNBT(registries, tag);
        tag.put("inputs", inputHandler.serializeNBT(registries));
        entropyTank.writeToNBT(registries, tag);
        tag.put("outputs", outputHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
    }
}
