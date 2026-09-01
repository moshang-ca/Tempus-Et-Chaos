package org.moshang.tempusetchaos.data;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class ChrononNetworkData extends SavedData {
    private static final String DATA_NAME = "chronon_networks";

    public static ChrononNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> ChrononNetworkData.create(level),
                        ((tag, provider) -> ChrononNetworkData.load(level, tag, provider))
                ),
                DATA_NAME
        );
    }

    public static ChrononNetworkData create(ServerLevel level) { return new ChrononNetworkData(level); }

    public static ChrononNetworkData load(ServerLevel level, CompoundTag tag, HolderLookup.Provider registries) {
        ChrononNetwork.NETWORK.clear();

        ListTag listTag = tag.getList("networks", ListTag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag networkTag = listTag.getCompound(i);
            ChrononNetwork.deserialize(networkTag, level);
        }
        return new ChrononNetworkData(level);
    }

    @Getter
    private final ServerLevel level;

    private ChrononNetworkData(ServerLevel level) {
        this.level = level;
    }

    public void save() {
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (ChrononNetwork network : ChrononNetwork.NETWORK.values()) {
            if (!network.isEmpty())
                listTag.add(network.serialize());
        }
        tag.put("networks", listTag);
        return tag;
    }
}
