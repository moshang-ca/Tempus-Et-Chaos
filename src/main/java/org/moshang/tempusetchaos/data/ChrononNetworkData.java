package org.moshang.tempusetchaos.data;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChrononNetworkData extends SavedData {
    private static final String DATA_NAME = "chronon_networks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ChrononNetworkData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> ChrononNetworkData.create(serverLevel),
                        ((tag, provider) -> ChrononNetworkData.load(serverLevel, tag, provider))
                ),
                DATA_NAME
        );
    }

    @Nullable
    public static ChrononNetwork getLevelNetwork(Level level, UUID uuid) {
        return get(level).getNetwork(uuid);
    }

    public static void addLevelNetwork(Level level, ChrononNetwork network) {
        get(level).addNetwork(network);
    }

    public static void removeLevelNetwork(Level level, UUID uuid) {
        get(level).removeNetwork(uuid);
    }

    public static ChrononNetworkData create(ServerLevel level) { return new ChrononNetworkData(level); }

    public static ChrononNetworkData load(ServerLevel level, CompoundTag tag, HolderLookup.Provider registries) {
        ChrononNetworkData networkData = new ChrononNetworkData(level);
        ListTag listTag = tag.getList("networks", ListTag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag networkTag = listTag.getCompound(i);
            ChrononNetwork network = ChrononNetwork.deserialize(networkTag, level);
            if (network != null)
                networkData.addNetwork(network);
        }
        return new ChrononNetworkData(level);
    }

    @Getter
    private final ServerLevel level;
    private final Map<UUID, ChrononNetwork> networks = new ConcurrentHashMap<>();

    private ChrononNetworkData(ServerLevel level) {
        this.level = level;
    }

    public void save() {
        setDirty();
    }

    public void addNetwork(ChrononNetwork network) {
        UUID uuid = network.getUuid();
        if (networks.containsKey(uuid)) {
            LOGGER.warn("Network ({}) has already exist, skip add", uuid);
            return;
        }
        networks.put(uuid, network);
        setDirty();
    }

    public void removeNetwork(UUID uuid) {
        networks.remove(uuid);
        setDirty();
    }

    @Nullable
    public ChrononNetwork getNetwork(UUID uuid) {
        return networks.get(uuid);
    }

    public boolean hasNetwork(UUID uuid) {
        return networks.containsKey(uuid);
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (ChrononNetwork network : networks.values()) {
            if (!network.isEmpty())
                listTag.add(network.serialize());
        }
        tag.put("networks", listTag);
        return tag;
    }
}
