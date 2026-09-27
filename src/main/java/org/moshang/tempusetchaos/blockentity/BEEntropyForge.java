package org.moshang.tempusetchaos.blockentity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.recipe.EntropyForgeRecipe;
import org.moshang.tempusetchaos.recipe.UniversalRecipeInput;
import org.moshang.tempusetchaos.registry.TECBlockEntities;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class BEEntropyForge extends BaseChrononNodeBlockEntity {
    public static final int INPUT_SLOTS = 3;
    public static final int OUTPUT_SLOT = 3;

    private static final long CHRONON_QUERY = Integer.MAX_VALUE;

    private boolean recipeDirty = true;
    private int progress = 0;
    private int retryCooldown = 0;
    private int saveCooldown = 0;

    @Getter
    private EntropyForgeRecipe recipe = null;

    @Getter
    private final FluidTank entropyTank = new FluidTank(50000, stack -> stack.is(TECFluids.GAS_ENTROPY_TYPE.get())) {
        @Override
        protected void onContentsChanged() {
            recipeDirty = true;
        }
    };
    @Getter
    private final IEnergyStorage energyStorage = new EnergyStorage(120000);
    @Getter
    private final ItemStackHandler itemHandler = new ItemStackHandler(INPUT_SLOTS + 1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < INPUT_SLOTS;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot < INPUT_SLOTS)
                recipeDirty = true;
        }
    };

    public BEEntropyForge(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.ENTROPY_FORGE_BE.get(), pos, blockState, 24000);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (recipeDirty) {
            recipeDirty = false;
            refreshRecipe();
        } else if (recipe == null && --retryCooldown <= 0) {
            refreshRecipe();
        }
        if (recipe == null) return;

        if (!canRun(recipe)) return;

        progress++;
        if (progress >= recipe.getProcessingTime()) {
            craft(recipe);
            return;
        }
        if (++saveCooldown >= 20) {
            saveCooldown = 0;
            setChanged();
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    private void refreshRecipe() {
        assert level != null;
        EntropyForgeRecipe found = level.getRecipeManager()
                .getRecipeFor(TECRecipes.ENTROPY_FORGE_RT.get(), buildInput(), level)
                .map(RecipeHolder::value)
                .orElse(null);
        retryCooldown = 20;
        if (found != recipe) {
            recipe = found;
            progress = 0;
            setChanged();
        }
    }

    private UniversalRecipeInput buildInput() {
        List<ItemStack> items = new ArrayList<>(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            items.add(itemHandler.getStackInSlot(slot));
        }
        List<FluidStack> fluids = List.of(entropyTank.getFluid());
        long chronon = innerNetwork == null ? 0 : innerNetwork.extractChronon(CHRONON_QUERY, true);
        int chrononInput = (int) Math.min(Integer.MAX_VALUE, chronon);
        return new UniversalRecipeInput(items, fluids, chrononInput, energyStorage.getEnergyStored());
    }

    private boolean canRun(EntropyForgeRecipe recipe) {
        if (energyStorage.getEnergyStored() < recipe.getEnergyCost()) return false;
        int chrononCost = recipe.getChrononCost();
        return chrononCost <= 0 || availableChronon() >= chrononCost;
    }

    private long availableChronon() {
        return innerNetwork == null ? 0 : innerNetwork.extractChronon(CHRONON_QUERY, true);
    }

    private void craft(EntropyForgeRecipe recipe) {
        assert level != null;
        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (!canAccept(result)) {
            progress = recipe.getProcessingTime() - 1;
            return;
        }

        consumeItems(recipe);

        int fluidCost = recipe.getFluidInputs().stream().mapToInt(FluidStack::getAmount).sum();
        if (fluidCost > 0) entropyTank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
        if (recipe.getChrononCost() > 0 && innerNetwork != null) {
            innerNetwork.extractChronon(recipe.getChrononCost(), false);
        }
        if (recipe.getEnergyCost() > 0) {
            energyStorage.extractEnergy(recipe.getEnergyCost(), false);
        }
        itemHandler.insertItem(OUTPUT_SLOT, result, false);

        progress = 0;
        recipeDirty = true;
        setChanged();
    }

    private void consumeItems(EntropyForgeRecipe recipe) {
        for (EntropyForgeRecipe.ItemRequirement requirement : recipe.getItemInputs()) {
            int needed = requirement.count();
            for (int slot = 0; slot < INPUT_SLOTS && needed > 0; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (stack.isEmpty() || !requirement.ingredient().test(stack)) continue;
                int take = Math.min(needed, stack.getCount());
                itemHandler.extractItem(slot, take, false);
                needed -= take;
            }
        }
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        entropyTank.readFromNBT(registries, tag);
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        progress = tag.getInt("progress");
        if (tag.contains("energy")) energyStorage.receiveEnergy(tag.getInt("energy"), false);
        recipeDirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        entropyTank.writeToNBT(registries, tag);
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }
}
