package org.moshang.tempusetchaos.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.recipe.EntropyForgeRecipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TECRecipeProvider extends RecipeProvider {
    public TECRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        super.buildRecipes(recipeOutput);
    }

    private static void entropyForging(RecipeOutput recipeOutput, List<EntropyForgeRecipe.ItemRequirement> inputs,
                                       List<FluidStack> fluidInputs, int ChrononCosts, int energyCosts, int processTime, ItemStack result) {


    }
}
