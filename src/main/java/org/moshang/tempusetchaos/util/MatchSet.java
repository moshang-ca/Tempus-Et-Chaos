package org.moshang.tempusetchaos.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public final class MatchSet {
    public static final Codec<MatchSet> CODEC = Entry.CODEC.listOf().xmap(MatchSet::fromEntries, MatchSet::ordered);

    private static MatchSet fromEntries(List<Entry> entries) {
        MatchSet mSet = new MatchSet();
        for (Entry entry : entries) {
            mSet.add(entry);
        }
        return mSet;
    }

    private final Set<Entry> entries = new LinkedHashSet<>();
    private final Map<Entry.Kind, Index> index = new EnumMap<>(Entry.Kind.class);

    public boolean add(Entry entry) {
        if (!entries.add(entry)) return false;
        Index idx = index.computeIfAbsent(entry.kind, k -> new Index());
        if (entry.isTag) idx.tags.add(create(entry.kind, entry.id));
        else idx.ids.add(entry.id);
        return true;
    }

    public boolean remove(Entry entry) {
        if (!entries.remove(entry)) return false;
        Index idx = index.get(entry.kind);
        if (idx == null) return false;
        if (entry.isTag) idx.tags.remove(create(entry.kind, entry.id));
        else idx.ids.remove(entry.id);
        if (idx.isEmpty()) index.remove(entry.kind);
        return true;
    }

    /**<b>This should only be used in the persist system</b>*/
    public void clear() {
        entries.clear();
        index.clear();
    }

    @SuppressWarnings("unchecked")
    public boolean matchBlock(ResourceLocation id, BlockState state) {
        Index idx = index.get(Entry.Kind.BLOCK);
        if (idx == null) return false;
        if (idx.ids.contains(id)) return true;
        for (var tagKey : idx.tags) {
            if (state.is((TagKey<Block>) tagKey)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean matchEntity(ResourceLocation id, EntityType<?> type) {
        Index idx = index.get(Entry.Kind.ENTITY);
        if (idx == null) return false;
        if (idx.ids.contains(id)) return true;
        for (TagKey<?> rawTag : idx.tags) {
            TagKey<EntityType<?>> tag = (TagKey<EntityType<?>>) rawTag;
            if (type.is(tag)) return true;
        }
        return false;
    }

    public boolean matchItem(ResourceLocation id, Item item) {
        // As we do not need item accessor yet.
        return false;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** The single display order: server and client both use it, so index based edits line up. */
    public List<Entry> ordered() {
        return List.copyOf(entries);
    }

    @SuppressWarnings("unchecked")
    private static <T> TagKey<T> create(Entry.Kind kind, ResourceLocation id) {
        ResourceKey<? extends Registry<T>> registry = (ResourceKey<? extends Registry<T>>) kind.registryFor();
        return TagKey.create(registry, id);
    }

    private static final class Index {
        final Set<ResourceLocation> ids = new HashSet<>();
        final Set<TagKey<?>> tags = new HashSet<>();

        boolean isEmpty() {
            return ids.isEmpty() && tags.isEmpty();
        }
    }

    @ParametersAreNonnullByDefault
    public record Entry(Kind kind, ResourceLocation id, boolean isTag) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Kind.CODEC.fieldOf("kind").forGetter(Entry::kind),
                    ResourceLocation.CODEC.fieldOf("id").forGetter(Entry::id),
                    Codec.BOOL.optionalFieldOf("is_tag", false).forGetter(Entry::isTag)
            ).apply(instance, Entry::new));

        public static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        Kind.STREAM_CODEC, Entry::kind,
                        ResourceLocation.STREAM_CODEC, Entry::id,
                        ByteBufCodecs.BOOL, Entry::isTag,
                        Entry::new
                );

        public static Entry fromItemStack(ItemStack itemStack) {
            Item item = itemStack.getItem();
            if (item instanceof SpawnEggItem egg) {
                return entity(BuiltInRegistries.ENTITY_TYPE.getKey(egg.getType(itemStack)));
            } else if (item instanceof BlockItem block) {
                return block(BuiltInRegistries.BLOCK.getKey(block.getBlock()));
            } else throw new IllegalArgumentException("Unknown item type: " + itemStack);
        }

        public static Entry block(ResourceLocation id) {
            return new Entry(Kind.BLOCK, id, false);
        }
        public static Entry blockTag(ResourceLocation id) {
            return new Entry(Kind.BLOCK, id, true);
        }
        public static Entry entity(ResourceLocation id) {
            return new Entry(Kind.ENTITY, id, false);
        }
        public static Entry entityTag(ResourceLocation id) {
            return new Entry(Kind.ENTITY, id, true);
        }

        /** Best effort display stack: an entry a screen can not draw shows as empty instead of failing. */
        public static ItemStack toItemStack(Entry entry) {
            // As we do not support tag filter yet.
            if (entry.isTag) return ItemStack.EMPTY;

            switch (entry.kind) {
                case BLOCK:
                    Item block = BuiltInRegistries.BLOCK.get(entry.id).asItem();
                    return block == Items.AIR ? ItemStack.EMPTY : new ItemStack(block);
                case ENTITY:
                    if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entry.id)) return ItemStack.EMPTY;
                    SpawnEggItem egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(entry.id));
                    return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
                default:
                    return ItemStack.EMPTY;
            }
        }

        public String serializedName() {
            return kind.name().toLowerCase(Locale.ROOT);
        }

        public enum Kind implements StringRepresentable {
            BLOCK, ENTITY, UNKNOWN;

            public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

            public static final StreamCodec<FriendlyByteBuf, Kind> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Kind.class);

            public static Kind fromItemStack(ItemStack itemStack) {
                if (itemStack.isEmpty()) return UNKNOWN;
                Item item = itemStack.getItem();
                return switch (item) {
                    case BlockItem block -> Kind.BLOCK;
                    case SpawnEggItem egg -> Kind.ENTITY;
                    default -> Kind.UNKNOWN;
                };
            }

            public ResourceKey<? extends Registry<?>> registryFor() {
                return switch (this) {
                    case BLOCK -> Registries.BLOCK;
                    case ENTITY -> Registries.ENTITY_TYPE;
                    default -> throw new IllegalStateException("Unknown registry for Entry");
                };
            }

            @Override
            @NotNull
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }
        }
    }
}
