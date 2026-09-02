package org.moshang.tempusetchaos.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.moshang.tempusetchaos.data.ChrononNetwork;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public abstract class BaseChrononNodeBlockEntity extends BlockEntity implements IChrononNode {
    protected UUID uuid;
    protected ChrononNetwork innerNetwork;
    protected final int capacity;

    public BaseChrononNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int chrononCapacity) {
        super(type, pos, blockState);
        this.capacity = chrononCapacity;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (innerNetwork == null) {
            innerNetwork = uuid == null ? null : ChrononNetwork.get(uuid);
        }
    }

    @Override
    public void setNetworkUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getNetworkUUID() {
        return uuid;
    }

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public int getCapacity() {
        return capacity;
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
