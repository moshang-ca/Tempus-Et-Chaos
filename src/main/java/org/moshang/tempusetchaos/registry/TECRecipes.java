package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.serializer.ProcessingRecipeSerializer;

import java.util.function.Supplier;

public class TECRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPE_DR = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, TempusEtChaos.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_DR = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TempusEtChaos.MODID);

    public static final Supplier<RecipeType<ProcessingRecipe.EntropyForging>> ENTROPY_FORGE_RT =
            RECIPE_TYPE_DR.register("entropy_forging", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "entropy_forging")));
    public static final Supplier<RecipeType<ProcessingRecipe.TimeExtracting>> TIME_EXTRACTOR_RT =
            RECIPE_TYPE_DR.register("time_extracting", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "time_extracting")));

    public static final Supplier<ProcessingRecipeSerializer<ProcessingRecipe.EntropyForging>> ENTROPY_FORGE_SERIALIZER =
            RECIPE_SERIALIZER_DR.register("entropy_forging", () -> new ProcessingRecipeSerializer<>(ProcessingRecipe.EntropyForging::new));
    public static final Supplier<ProcessingRecipeSerializer<ProcessingRecipe.TimeExtracting>> TIME_EXTRACTOR_SERIALIZER =
            RECIPE_SERIALIZER_DR.register("time_extracting", () -> new ProcessingRecipeSerializer<>(ProcessingRecipe.TimeExtracting::new));
}
