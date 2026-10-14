package org.moshang.tempusetchaos.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.recipe.ItemRequirement;
import org.moshang.tempusetchaos.recipe.TimeExtractorRecipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TECRecipeProvider extends RecipeProvider {
    public TECRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        timeExtracting(recipeOutput, "cobblestone", TimeExtractorRecipe.builder()
                .inputItem(itemOf(Items.COBBLESTONE, 1))
                .outputChronon(20)
                .processingTime(200)
                .perTick(true)
                .build());
    }

    private static void entropyForging(RecipeOutput recipeOutput, List<ItemRequirement> inputs,
                                       List<FluidStack> fluidInputs, int ChrononCosts, int energyCosts, int processTime, ItemStack result) {

    }

    private static void timeExtracting(RecipeOutput recipeOutput, String name, TimeExtractorRecipe recipe) {
        recipeOutput.accept(TempusEtChaos.id("time_extracting/" + name), recipe, null);
    }

    private static ItemRequirement itemOf(ItemLike item, int count) {
        return new ItemRequirement(Ingredient.of(item), count);
    }

    private static ItemRequirement itemOf(TagKey<Item> tag, int count) {
        return new ItemRequirement(Ingredient.of(tag), count);
    }
}
