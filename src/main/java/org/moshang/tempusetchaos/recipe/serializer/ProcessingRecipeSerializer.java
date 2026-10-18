package org.moshang.tempusetchaos.recipe.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ItemResult;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Results;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.ValueQuantity;
import org.moshang.tempusetchaos.recipe.IProcessingRecipe.Values;
import org.moshang.tempusetchaos.recipe.ProcessingRecipe;
import org.moshang.tempusetchaos.recipe.RecipeMaterials;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.ItemRequirement;
import org.moshang.tempusetchaos.util.TECConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Reads and writes the recipes of one type. A type already knows the type and serializer it is registered under, so all
 * this needs is how to build that type; the JSON shape itself is shared by every processing recipe.
 * <p>
 * The material layer is written flat, one list per kind and direction, and the value layer sits beside it: the four
 * lists are what JEI can show, the four values are numbers the machine accounts for itself.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ProcessingRecipeSerializer<R extends ProcessingRecipe> implements RecipeSerializer<R> {
    @FunctionalInterface
    public interface Factory<R extends ProcessingRecipe> {
        R create(int processingTime, RecipeMaterials inputs, Results outputs, Values valuesIn, Values valuesOut);
    }

    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

    public ProcessingRecipeSerializer(Factory<R> factory) {
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("time", TECConstants.DEFAULT_PROCESS_TIME).forGetter(ProcessingRecipe::processingTime),
                ItemRequirement.CODEC.listOf().optionalFieldOf("item_inputs", List.of())
                        .forGetter(recipe -> recipe.inputs().items()),
                FluidAmount.CODEC.listOf().optionalFieldOf("fluid_inputs", List.of())
                        .forGetter(recipe -> recipe.inputs().fluids()),
                ItemResult.CODEC.listOf().optionalFieldOf("item_outputs", List.of())
                        .forGetter(recipe -> recipe.outputs().items()),
                FluidAmount.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of())
                        .forGetter(recipe -> recipe.outputs().fluids()),
                ValueQuantity.CODEC.optionalFieldOf("energy_cost", ValueQuantity.NONE)
                        .forGetter(recipe -> recipe.valuesIn().energy()),
                ValueQuantity.CODEC.optionalFieldOf("chronon_cost", ValueQuantity.NONE)
                        .forGetter(recipe -> recipe.valuesIn().chronon()),
                ValueQuantity.CODEC.optionalFieldOf("energy_output", ValueQuantity.NONE)
                        .forGetter(recipe -> recipe.valuesOut().energy()),
                ValueQuantity.CODEC.optionalFieldOf("chronon_output", ValueQuantity.NONE)
                        .forGetter(recipe -> recipe.valuesOut().chronon())
        ).apply(instance, (time, itemInputs, fluidInputs, itemOutputs, fluidOutputs, energyIn, chrononIn, energyOut, chrononOut) ->
                factory.create(time,
                        new RecipeMaterials(itemInputs, fluidInputs),
                        new Results(itemOutputs, fluidOutputs),
                        new Values(energyIn, chrononIn),
                        new Values(energyOut, chrononOut))));

        this.streamCodec = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ProcessingRecipe::processingTime,
                RecipeMaterials.STREAM_CODEC, ProcessingRecipe::inputs,
                Results.STREAM_CODEC, ProcessingRecipe::outputs,
                Values.STREAM_CODEC, ProcessingRecipe::valuesIn,
                Values.STREAM_CODEC, ProcessingRecipe::valuesOut,
                factory::create);
    }

    @Override
    public MapCodec<R> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }
}
