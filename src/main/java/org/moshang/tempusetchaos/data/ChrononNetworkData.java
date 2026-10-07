package org.moshang.tempusetchaos.data;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.blockentity.network.ChrononNetwork;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChrononNetworkData extends SavedData {
    private static final String DATA_NAME = "chronon_networks";
    private static final int SYNC_INTERVAL = 5;

    public static ChrononNetworkData get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> ChrononNetworkData.create(serverLevel),
                        ((tag, provider) -> ChrononNetworkData.load(serverLevel, tag))
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

    public static ChrononNetworkData load(ServerLevel level, CompoundTag tag) {
        ChrononNetworkData networkData = new ChrononNetworkData(level);
        ListTag listTag = tag.getList("networks", ListTag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag networkTag = listTag.getCompound(i);
            ChrononNetwork network = ChrononNetwork.deserialize(networkTag, level);
            if (network != null)
                networkData.addNetwork(network);
        }
        return networkData;
    }

    @Getter
    private final ServerLevel level;
    private final Map<UUID, ChrononNetwork> networks = new ConcurrentHashMap<>();

    private int lastSync = -1;

    private ChrononNetworkData(ServerLevel level) {
        this.level = level;
    }

    public void serverTick() {
        if (lastSync >= SYNC_INTERVAL) {
            networks.values().forEach(n -> n.syncToObservers(false));
            lastSync = -1;
        }
        lastSync++;
    }

    public void save() {
        setDirty();
    }

    public void addNetwork(ChrononNetwork network) {
        UUID uuid = network.getUuid();
        if (networks.containsKey(uuid)) {
            log.warn("Network ({}) has already exist, skip add", uuid);
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

    public boolean isEmpty() {
        return networks.isEmpty();
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
