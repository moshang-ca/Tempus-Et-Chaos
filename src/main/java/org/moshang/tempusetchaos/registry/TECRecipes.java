package org.moshang.tempusetchaos.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.recipe.EntropyForgeRecipe;
import org.moshang.tempusetchaos.recipe.serializer.EntropyForgeRecipeSerializer;

import java.util.function.Supplier;

public class TECRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPE_DR = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, TempusEtChaos.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_DR = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TempusEtChaos.MODID);

    public static final Supplier<RecipeType<EntropyForgeRecipe>> ENTROPY_FORGE_RT =
            RECIPE_TYPE_DR.register("entropy_forging", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "entropy_forging")));

    public static final Supplier<EntropyForgeRecipeSerializer> ENTROPY_FORGE_SERIALIZER =
            RECIPE_SERIALIZER_DR.register("entropy_forging", EntropyForgeRecipeSerializer::new);
}
