package org.moshang.tempusetchaos.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.registry.TECBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BEVirtualNode extends BaseChrononNodeBlockEntity {
    public BEVirtualNode(BlockPos pos, BlockState blockState) {
        super(TECBlockEntities.VIRTUAL_NODE_BE.get(), pos, blockState, 1000);
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PRODUCER;
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
