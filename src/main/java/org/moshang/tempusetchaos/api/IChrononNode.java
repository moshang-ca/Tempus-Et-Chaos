package org.moshang.tempusetchaos.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.moshang.tempusetchaos.data.ChrononNetwork;

import java.util.Set;
import java.util.UUID;

public interface IChrononNode {
    void setNetworkUUID(UUID uuid);

    UUID getNetworkUUID();
    Level getLevel();
    BlockPos getNodePos();
    NodeType getNodeType();
    int getProduced();
    int getConsumed();
    int getCapacity();

    static void onNodePlaced(Level level, BlockPos pos, IChrononNode node) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        Set<UUID> adjacent = ChrononNetwork.findAdjacent(serverLevel, pos);
        if (adjacent.isEmpty()) {
            ChrononNetwork network = new ChrononNetwork(serverLevel);
            network.addNode(node);
            node.setNetworkUUID(network.getUuid());

        } else if (adjacent.size() == 1) {
            UUID targetUuid = adjacent.iterator().next();
            ChrononNetwork targetNetwork = ChrononNetwork.NETWORK.get(targetUuid);
            if (targetNetwork != null) {
                targetNetwork.addNode(node);
                node.setNetworkUUID(targetUuid);
            }
        } else {
            ChrononNetwork primary = null;
            for (UUID id : adjacent) {
                ChrononNetwork network = ChrononNetwork.NETWORK.get(id);
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

    enum NodeType {
        PRODUCER,
        CONSUMER,
        STORAGE
    }
}
