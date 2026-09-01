package org.moshang.tempusetchaos.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.tempusetchaos.api.IChrononNode;
import org.moshang.tempusetchaos.registry.TECBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class BEVirtualNode extends BlockEntity implements IChrononNode {
    private UUID uuid;

    public BEVirtualNode(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.VIRTUAL_NODE_BE.get(), pos, blockState);
    }

    @Override
    public void setNetworkUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getNetworkUUID() {
        return this.uuid;
    }

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.CONSUMER;
    }

    @Override
    public int getProduced() {
        return 10;
    }

    @Override
    public int getConsumed() {
        return 0;
    }

    @Override
    public int getCapacity() {
        return 100;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("network_uuid")) {
            this.uuid = tag.getUUID("network_uuid");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.uuid != null) {
            tag.putUUID("network_uuid", uuid);
        }
    }
}
