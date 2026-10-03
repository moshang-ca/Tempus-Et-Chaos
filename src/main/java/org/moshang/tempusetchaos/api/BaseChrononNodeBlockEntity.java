package org.moshang.tempusetchaos.api;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.blockentity.network.ChrononNetwork;
import org.moshang.tempusetchaos.data.ChrononNetworkData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public abstract class BaseChrononNodeBlockEntity extends BlockEntity implements IChrononNode {
    protected UUID uuid;
    protected ChrononNetwork innerNetwork;
    @Getter
    protected final int capacity;

    public BaseChrononNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int chrononCapacity) {
        super(type, pos, blockState);
        this.capacity = chrononCapacity;
    }

    /**
     * Implement server logic in this method.
     * <p>Default implementation has ensured this will run on the server side</>
     * */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (innerNetwork == null) {
            innerNetwork = uuid == null ? null : ChrononNetworkData.getLevelNetwork(level, uuid);
        }
    }

    @Override
    public void setNetworkUUID(UUID uuid) {
        this.uuid = uuid;
        innerNetwork = null;
    }

    @Override
    public UUID getNetworkUUID() {
        return uuid;
    }

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    public long getChrononStored() {
        ChrononNetwork network = network();
        return network == null ? 0 : network.getChrononStored();
    }

    public long getChrononCapacity() {
        ChrononNetwork network = network();
        return network == null ? 0 : network.getCapacity();
    }

    @Nullable
    private ChrononNetwork network() {
        if (innerNetwork == null && uuid != null && level instanceof ServerLevel serverLevel) {
            innerNetwork = ChrononNetworkData.getLevelNetwork(serverLevel, uuid);
        }
        return innerNetwork;
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
