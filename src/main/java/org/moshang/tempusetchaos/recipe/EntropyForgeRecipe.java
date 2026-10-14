package org.moshang.tempusetchaos.recipe;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.moshang.tempusetchaos.recipe.input.UniversalRecipeInput;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntropyForgeRecipe implements Recipe<UniversalRecipeInput>, ITimeConsumable {
    private final List<ItemRequirement> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final int chrononCost;
    private final int energyCost;
    private final int processingTime;
    private final ItemStack result;

    public EntropyForgeRecipe(List<ItemRequirement> itemInputs, List<FluidStack> fluidInputs,
                              int chrononCost, int energyCost, int processingTime, ItemStack result) {
        if (itemInputs.isEmpty() || itemInputs.size() > 3) {
            throw new IllegalArgumentException("item inputs must be 1..3, got " + itemInputs.size());
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("result must not be empty");
        }
        for (FluidStack fluid : fluidInputs) {
            if (fluid.isEmpty()) throw new IllegalArgumentException("fluid inputs must not be empty");
        }
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = fluidInputs.stream().map(FluidStack::copy).toList();
        this.chrononCost = Math.max(0, chrononCost);
        this.energyCost = Math.max(0, energyCost);
        this.processingTime = Math.max(1, processingTime);
        this.result = result.copy();
    }

    @Override
    public boolean matches(UniversalRecipeInput input, Level level) {
        return itemsMatch(input) && fluidsMatch(input) && energyMatch(input);
    }

    private boolean itemsMatch(UniversalRecipeInput input) {
        int[] remaining = new int[input.size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = input.getItem(i).getCount();
        }
        for (ItemRequirement requirement : itemInputs) {
            int needed = requirement.count();
            for (int i = 0; i < remaining.length && needed > 0; i++) {
                ItemStack stack = input.getItem(i);
                if (remaining[i] <= 0 || stack.isEmpty() || !requirement.ingredient().test(stack)) continue;
                int take = Math.min(needed, remaining[i]);
                remaining[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private boolean fluidsMatch(UniversalRecipeInput input) {
        int[] remaining = new int[input.fluidInputs().size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = input.getFluidInput(i).getAmount();
        }
        for (FluidStack requirement : fluidInputs) {
            int needed = requirement.getAmount();
            for (int i = 0; i < remaining.length && needed > 0; i++) {
                FluidStack stack = input.getFluidInput(i);
                if (remaining[i] <= 0 || stack.isEmpty() || !requirement.is(stack.getFluid())) continue;
                int take = Math.min(needed, remaining[i]);
                remaining[i] -= take;
                needed -= take;
            }
            if (needed > 0) return false;
        }
        return true;
    }

    private boolean energyMatch(UniversalRecipeInput input) {
        return input.chronon() >= chrononCost && input.energy() >= energyCost;
    }

    @Override
    public boolean isPerTick() {
        return false;
    }

    @Override
    public ItemStack assemble(UniversalRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (ItemRequirement requirement : itemInputs) {
            ingredients.add(requirement.ingredient());
        }
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TECRecipes.ENTROPY_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TECRecipes.ENTROPY_FORGE_RT.get();
    }

}
