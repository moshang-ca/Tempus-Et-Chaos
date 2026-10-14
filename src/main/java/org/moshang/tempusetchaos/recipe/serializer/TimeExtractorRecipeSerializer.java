package org.moshang.tempusetchaos.recipe.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.moshang.tempusetchaos.recipe.ItemRequirement;
import org.moshang.tempusetchaos.recipe.TimeExtractorRecipe;
import org.moshang.tempusetchaos.util.TECConstants;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeExtractorRecipeSerializer implements RecipeSerializer<TimeExtractorRecipe> {
    public static final MapCodec<TimeExtractorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemRequirement.CODEC.fieldOf("input").forGetter(TimeExtractorRecipe::getInputItem),
            Codec.INT.optionalFieldOf("time", TECConstants.DEFAULT_PROCESS_TIME).forGetter(TimeExtractorRecipe::getProcessingTime),
            Codec.INT.fieldOf("output").forGetter(TimeExtractorRecipe::getOutputChronon),
            Codec.BOOL.optionalFieldOf("per_tick", false).forGetter(TimeExtractorRecipe::isPerTick)
    ).apply(instance, TimeExtractorRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimeExtractorRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemRequirement.STREAM_CODEC, TimeExtractorRecipe::getInputItem,
            ByteBufCodecs.INT, TimeExtractorRecipe::getProcessingTime,
            ByteBufCodecs.INT, TimeExtractorRecipe::getOutputChronon,
            ByteBufCodecs.BOOL, TimeExtractorRecipe::isPerTick,
            TimeExtractorRecipe::new
    );

    @Override
    public MapCodec<TimeExtractorRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, TimeExtractorRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
