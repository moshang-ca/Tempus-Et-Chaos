package org.moshang.tempusetchaos.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * The material layer of what a machine takes in: the things it matches on and really moves out of its storage.
 * Values (energy, chronon) stay out of it.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record RecipeMaterials(List<ItemRequirement> items, List<FluidAmount> fluids) {
    public static final RecipeMaterials EMPTY = new RecipeMaterials(List.of(), List.of());

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeMaterials> STREAM_CODEC = StreamCodec.composite(
            ItemRequirement.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeMaterials::items,
            FluidAmount.STREAM_CODEC.apply(ByteBufCodecs.list()), RecipeMaterials::fluids,
            RecipeMaterials::new);

    public RecipeMaterials {
        items = List.copyOf(items);
        fluids = List.copyOf(fluids);
    }

    public boolean isEmpty() {
        return items.isEmpty() && fluids.isEmpty();
    }

    /**
     * An item ingredient. {@code count} is the whole cost, or the cost of one tick when {@code perTick} is set, and
     * {@code chance} is the chance of that cost happening at all, 1 being certain.
     */
    public record ItemRequirement(Ingredient ingredient, int count, boolean perTick, float chance) {
        public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemRequirement::ingredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(ItemRequirement::count),
                Codec.BOOL.optionalFieldOf("per_tick", false).forGetter(ItemRequirement::perTick),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(ItemRequirement::chance)
        ).apply(instance, ItemRequirement::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemRequirement> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ItemRequirement::ingredient,
                ByteBufCodecs.VAR_INT, ItemRequirement::count,
                ByteBufCodecs.BOOL, ItemRequirement::perTick,
                ByteBufCodecs.FLOAT, ItemRequirement::chance,
                ItemRequirement::new);

        public ItemRequirement {
            if (count <= 0) throw new IllegalArgumentException("count must be > 0");
            chance = Mth.clamp(chance, 0f, 1f);
        }

        public ItemRequirement scaled(int factor) {
            return new ItemRequirement(ingredient, count * factor, perTick, chance);
        }
    }

    /** A fluid amount in mB. The same shape serves a cost and a result. */
    public record FluidAmount(FluidStack stack, boolean perTick, float chance) {
        public static final Codec<FluidAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FluidStack.CODEC.fieldOf("stack").forGetter(FluidAmount::stack),
                Codec.BOOL.optionalFieldOf("per_tick", false).forGetter(FluidAmount::perTick),
                Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(FluidAmount::chance)
        ).apply(instance, FluidAmount::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidAmount> STREAM_CODEC = StreamCodec.composite(
                FluidStack.STREAM_CODEC, FluidAmount::stack,
                ByteBufCodecs.BOOL, FluidAmount::perTick,
                ByteBufCodecs.FLOAT, FluidAmount::chance,
                FluidAmount::new);

        public FluidAmount {
            if (stack.isEmpty()) throw new IllegalArgumentException("fluid stack must not be empty");
            chance = Mth.clamp(chance, 0f, 1f);
        }

        public FluidAmount scaled(int factor) {
            return new FluidAmount(stack.copyWithAmount(stack.getAmount() * factor), perTick, chance);
        }
    }
}
