package org.moshang.tempusetchaos.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public record ItemRequirement(Ingredient ingredient, int count) {
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(ItemRequirement::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemRequirement::count)
    ).apply(instance, ItemRequirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemRequirement> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, ItemRequirement::ingredient,
            ByteBufCodecs.VAR_INT, ItemRequirement::count,
            ItemRequirement::new);

    public ItemRequirement {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
    }
}
