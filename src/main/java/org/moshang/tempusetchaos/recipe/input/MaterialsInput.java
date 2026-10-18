package org.moshang.tempusetchaos.recipe.input;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/** What a machine offers for matching: the material layer only, values are never part of it. */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record MaterialsInput(List<ItemStack> items, List<FluidStack> fluids) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    public FluidStack getFluid(int index) {
        if (index < 0 || index >= fluids.size()) return FluidStack.EMPTY;
        return fluids.get(index);
    }
}
