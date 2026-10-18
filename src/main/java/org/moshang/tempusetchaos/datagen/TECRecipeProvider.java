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
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ItemResult;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Results;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ValueQuantity;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Values;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.RecipeMaterials;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.registry.TECFluids;
import org.moshang.tempusetchaos.registry.TECItems;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TECRecipeProvider extends RecipeProvider {
    public TECRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        timeExtracting(recipeOutput, "cobblestone", 10,
                itemOf(Items.COBBLESTONE, 1), new ValueQuantity(20, true));

        entropyForging(recipeOutput, "entropy_crystal", 100,
                List.of(itemOf(Items.IRON_INGOT, 1), itemOf(Items.AMETHYST_SHARD, 1)),
                List.of(fluidOf(TECFluids.GAS_ENTROPY_SOURCE.get(), 1000)),
                List.of(new ItemResult(new ItemStack(TECItems.ENTROPY_CRYSTAL.get()), false, 1f),
                        new ItemResult(new ItemStack(Items.QUARTZ), false, .25f)),
                new Values(new ValueQuantity(20, true), new ValueQuantity(5, true)));
    }

    private static void timeExtracting(RecipeOutput recipeOutput, String name, int time,
                                       ItemRequirement input, ValueQuantity chrononOutput) {
        ProcessingRecipe recipe = new ProcessingRecipe.TimeExtracting(time,
                new RecipeMaterials(List.of(input), List.of()), Results.EMPTY, Values.NONE,
                new Values(ValueQuantity.NONE, chrononOutput));
        recipeOutput.accept(TempusEtChaos.id("time_extracting/" + name), recipe, null);
    }

    private static void entropyForging(RecipeOutput recipeOutput, String name, int time, List<ItemRequirement> items,
                                       List<FluidAmount> fluids, List<ItemResult> results, Values valuesIn) {
        ProcessingRecipe recipe = new ProcessingRecipe.EntropyForging(time,
                new RecipeMaterials(items, fluids), new Results(results, List.of()), valuesIn, Values.NONE);
        recipeOutput.accept(TempusEtChaos.id("entropy_forging/" + name), recipe, null);
    }

    private static ItemRequirement itemOf(ItemLike item, int count) {
        return new ItemRequirement(Ingredient.of(item), count, false, 1f);
    }

    private static ItemRequirement itemOf(TagKey<Item> tag, int count) {
        return new ItemRequirement(Ingredient.of(tag), count, false, 1f);
    }

    private static FluidAmount fluidOf(net.minecraft.world.level.material.Fluid fluid, int amount) {
        return new FluidAmount(new FluidStack(fluid, amount), false, 1f);
    }
}
