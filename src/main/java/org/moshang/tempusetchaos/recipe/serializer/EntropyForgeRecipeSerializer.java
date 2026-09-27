package org.moshang.tempusetchaos.recipe.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;
import org.moshang.tempusetchaos.recipe.EntropyForgeRecipe;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntropyForgeRecipeSerializer implements RecipeSerializer<EntropyForgeRecipe> {
    public static final MapCodec<EntropyForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntropyForgeRecipe.ItemRequirement.CODEC.listOf()
                    .fieldOf("items").forGetter(EntropyForgeRecipe::getItemInputs),
            FluidStack.CODEC.listOf().fieldOf("fluids").forGetter(EntropyForgeRecipe::getFluidInputs),
            Codec.INT.optionalFieldOf("chronon", 0).forGetter(EntropyForgeRecipe::getChrononCost),
            Codec.INT.optionalFieldOf("energy", 0).forGetter(EntropyForgeRecipe::getEnergyCost),
            Codec.INT.optionalFieldOf("time", EntropyForgeRecipe.DEFAULT_TIME).forGetter(EntropyForgeRecipe::getProcessingTime),
            ItemStack.CODEC.fieldOf("result").forGetter(EntropyForgeRecipe::getResult)
    ).apply(instance, EntropyForgeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntropyForgeRecipe> STREAM_CODEC = StreamCodec.composite(
            EntropyForgeRecipe.ItemRequirement.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
            EntropyForgeRecipe::getItemInputs,
            FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()), EntropyForgeRecipe::getFluidInputs,
            ByteBufCodecs.VAR_INT, EntropyForgeRecipe::getChrononCost,
            ByteBufCodecs.VAR_INT, EntropyForgeRecipe::getEnergyCost,
            ByteBufCodecs.VAR_INT, EntropyForgeRecipe::getProcessingTime,
            ItemStack.STREAM_CODEC, EntropyForgeRecipe::getResult,
            EntropyForgeRecipe::new);

    @Override
    public MapCodec<EntropyForgeRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EntropyForgeRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
