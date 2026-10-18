package org.moshang.tempusetchaos.recipe;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.moshang.tempusetchaos.registry.TECRecipes;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The common shape of every machine recipe: the material layer the machine matches and moves, and the value layer it
 * accounts for on its own. One subclass stands for one recipe type, and it names the type and serializer it is
 * registered under, so nothing that builds a recipe has to carry either around.
 */
@Getter
@Accessors(fluent = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ProcessingRecipe implements IProcessingRecipe {
    private final int processingTime;
    private final RecipeMaterials inputs;
    private final Results outputs;
    private final Values valuesIn;
    private final Values valuesOut;

    protected ProcessingRecipe(int processingTime, RecipeMaterials inputs, Results outputs,
                               Values valuesIn, Values valuesOut) {
        if (processingTime <= 0) throw new IllegalArgumentException("processing time must be > 0");
        this.processingTime = processingTime;
        this.inputs = inputs;
        this.outputs = outputs;
        this.valuesIn = valuesIn;
        this.valuesOut = valuesOut;
    }

    /** Extracting time from a material: item inputs in, chronon out. */
    public static final class TimeExtracting extends ProcessingRecipe {
        public TimeExtracting(int processingTime, RecipeMaterials inputs, Results outputs,
                              Values valuesIn, Values valuesOut) {
            super(processingTime, inputs, outputs, valuesIn, valuesOut);
        }

        @Override
        public RecipeType<?> getType() {
            return TECRecipes.TIME_EXTRACTOR_RT.get();
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TECRecipes.TIME_EXTRACTOR_SERIALIZER.get();
        }
    }

    /** Forging with entropy: item and fluid inputs in, items and fluids out, paid for in energy and chronon. */
    public static final class EntropyForging extends ProcessingRecipe {
        public EntropyForging(int processingTime, RecipeMaterials inputs, Results outputs,
                              Values valuesIn, Values valuesOut) {
            super(processingTime, inputs, outputs, valuesIn, valuesOut);
        }

        @Override
        public RecipeType<?> getType() {
            return TECRecipes.ENTROPY_FORGE_RT.get();
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TECRecipes.ENTROPY_FORGE_SERIALIZER.get();
        }
    }
}
