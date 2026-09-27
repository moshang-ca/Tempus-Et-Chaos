package org.moshang.tempusetchaos.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record UniversalRecipeInput(
        List<ItemStack> inputs,
        List<FluidStack> fluidInputs,
        int chronon,
        int energy
) implements RecipeInput {
    @Override
    @NotNull
    public ItemStack getItem(int index) {
        if (index < 0 || index >= inputs.size()) return ItemStack.EMPTY;
        return inputs.get(index);
    }

    @Override
    public int size() {
        return inputs.size();
    }

    public FluidStack getFluidInput(int index) {
        if (index < 0 || index >= fluidInputs.size()) return FluidStack.EMPTY;
        return fluidInputs.get(index);
    }
}
