package org.moshang.tempusetchaos.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.moshang.tempusetchaos.data.ChrononNetwork;
import org.moshang.tempusetchaos.data.ChrononNetworkData;

import java.util.Set;
import java.util.UUID;

public interface IChrononNode extends ICableConnectable {
    void setNetworkUUID(UUID uuid);

    UUID getNetworkUUID();
    Level getLevel();
    BlockPos getNodePos();
    NodeType getNodeType();

    default int getProduced() { return 0; }
    default int getConsumed() { return 0; }
    default int getCapacity() { return 0; }

    static void onNodePlaced(Level level, BlockPos pos, IChrononNode node) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Set<UUID> adjacent = ChrononNetwork.findAdjacent(serverLevel, pos);
        if (adjacent.isEmpty()) {
            ChrononNetwork network = new ChrononNetwork(serverLevel);
            network.addNode(node);
            node.setNetworkUUID(network.getUuid());
            ChrononNetworkData.addLevelNetwork(level, network);
        } else if (adjacent.size() == 1) {
            UUID targetUuid = adjacent.iterator().next();
            ChrononNetwork targetNetwork = ChrononNetworkData.getLevelNetwork(level, targetUuid);
            if (targetNetwork != null) {
                targetNetwork.addNode(node);
                node.setNetworkUUID(targetUuid);
            }
        } else {
            ChrononNetwork primary = null;
            for (UUID id : adjacent) {
                ChrononNetwork network = ChrononNetworkData.getLevelNetwork(level, id);
                if (network == null) continue;
                if (primary == null) {
                    primary = network;
                } else {
                    primary.merge(id);
                }
            }
            if (primary != null) {
                primary.addNode(node);
                node.setNetworkUUID(primary.getUuid());
            }
        }
    }

    static void onNodeRemoved(Level level, BlockPos pos, IChrononNode node) {
        if (!(level instanceof ServerLevel)) return;

        ChrononNetwork network = ChrononNetworkData.getLevelNetwork(level, node.getNetworkUUID());
        if (network == null) return;
        network.removeNode(node);
    }

    enum NodeType {
        PRODUCER,
        CONSUMER,
        STORAGE
    }
}
