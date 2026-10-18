package org.moshang.tempusetchaos.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.moshang.tempusetchaos.recipe.RecipeMaterials.FluidAmount;
import org.moshang.tempusetchaos.recipe.input.MaterialsInput;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * A recipe a machine runs over time: what it matches on and moves (the material layer) and the numbers only the
 * running machine can account for (the value layer). A value never takes part in matching.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface IProcessingRecipe extends Recipe<MaterialsInput> {
    /** Base duration in ticks, before the machine's own speed modifiers. */
    int processingTime();

    RecipeMaterials inputs();

    Results outputs();

    Values valuesIn();

    Values valuesOut();

    /** Matching only ever looks at the material layer. */
    @Override
    default boolean matches(MaterialsInput input, Level level) {
        return MaterialLedger.of(input).canTake(inputs(), 1);
    }

    /** A value of the value layer: a whole-run amount, or the amount of one tick when {@code perTick} is set. */
    record ValueQuantity(int amount, boolean perTick) {
        public static final ValueQuantity NONE = new ValueQuantity(0, false);

        public static final Codec<ValueQuantity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("amount", 0).forGetter(ValueQuantity::amount),
                Codec.BOOL.optionalFieldOf("per_tick", false).forGetter(ValueQuantity::perTick)
        ).apply(instance, ValueQuantity::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ValueQuantity> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ValueQuantity::amount,
                ByteBufCodecs.BOOL, ValueQuantity::perTick,
                ValueQuantity::new);

        public ValueQuantity {
            if (amount < 0) throw new IllegalArgumentException("amount must not be negative");
        }

        public boolean isEmpty() {
            return amount == 0;
        }

        /** The amount one machine pays or receives, after its parallel count and its value scale. */
        public long amount(int parallel, float scale) {
            return Math.round(amount * (double) parallel * scale);
        }

        /** The amount over a whole run of {@code time} ticks. */
        public long total(int time, int parallel, float scale) {
            return perTick ? amount(parallel, scale) * time : amount(parallel, scale);
        }
    }

    /** The two value channels of a recipe. */
    record Values(ValueQuantity energy, ValueQuantity chronon) {
        public static final Values NONE = new Values(ValueQuantity.NONE, ValueQuantity.NONE);

        public static final StreamCodec<RegistryFriendlyByteBuf, Values> STREAM_CODEC = StreamCodec.composite(
                ValueQuantity.STREAM_CODEC, Values::energy,
                ValueQuantity.STREAM_CODEC, Values::chronon,
                Values::new);

        public boolean isEmpty() {
            return energy.isEmpty() && chronon.isEmpty();
        }
    }

    /** The material layer of what a machine gives back. A result is a real stack, never an ingredient. */
    record Results(List<ItemResult> items, List<FluidAmount> fluids) {
        public static final Results EMPTY = new Results(List.of(), List.of());

        public static final StreamCodec<RegistryFriendlyByteBuf, Results> STREAM_CODEC = StreamCodec.composite(
                ItemResult.STREAM_CODEC.apply(ByteBufCodecs.list()), Results::items,
                FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), Results::fluids,
                Results::new);

        public Results {
            items = List.copyOf(items);
            fluids = List.copyOf(fluids);
        }

        public boolean isEmpty() {
            return items.isEmpty() && fluids.isEmpty();
        }
    }

    /** An item a recipe gives back, timed and rolled like an item cost. */
    record ItemResult(ItemStack stack, boolean perTick, float chance) {
        public static final Codec<ItemResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(ItemResult::stack),
                Codec.BOOL.optionalFieldOf("per_tick", false).forGetter(ItemResult::perTick),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(ItemResult::chance)
        ).apply(instance, ItemResult::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemResult> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ItemResult::stack,
                ByteBufCodecs.BOOL, ItemResult::perTick,
                ByteBufCodecs.FLOAT, ItemResult::chance,
                ItemResult::new);

        public ItemResult {
            if (stack.isEmpty()) throw new IllegalArgumentException("result must not be empty");
            chance = Mth.clamp(chance, 0f, 1f);
        }

        public ItemResult scaled(int factor) {
            return new ItemResult(stack.copyWithCount(stack.getCount() * factor), perTick, chance);
        }
    }

    @Override
    default ItemStack assemble(MaterialsInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    /** The first item result, for the vanilla ways of asking a recipe what it makes. */
    @Override
    default ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs().items().isEmpty() ? ItemStack.EMPTY : outputs().items().getFirst().stack().copy();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    default NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (RecipeMaterials.ItemRequirement requirement : inputs().items()) {
            ingredients.add(requirement.ingredient());
        }
        return ingredients;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }
}
