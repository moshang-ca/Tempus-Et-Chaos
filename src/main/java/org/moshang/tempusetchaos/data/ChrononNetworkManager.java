package org.moshang.tempusetchaos.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IChrononStorage;

import java.util.*;

public class ChrononNetworkManager {
    private final ServerLevel level;
    private final Map<BlockPos, INetworkNode> nodeMap = new HashMap<>();
    private final List<Set<BlockPos>> networks = new ArrayList<>();

    private boolean dirty = false;
    private long lastRebuildTime = 0;

    public ChrononNetworkManager(ServerLevel level) {
        this.level = level;
    }



    public interface INetworkNode {
        BlockPos getNodePos();

        @Nullable
        default IChrononStorage getStorage() { return null; }

        default boolean isProducer() { return false; }
        default boolean isConsumer() { return false; }
        default Set<BlockPos> getNeighbors() { return Collections.emptySet(); }
    }
}
