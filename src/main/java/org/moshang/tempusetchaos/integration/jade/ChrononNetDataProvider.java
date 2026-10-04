package org.moshang.tempusetchaos.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.IChrononNode;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ChrononNetDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final String TAG = "chronon";
    public static final String STORED = "stored";
    public static final String CAPACITY = "capacity";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getTarget() instanceof IChrononNode node)) return;
        long capacity = node.getNetChrononCapacity();
        if (capacity <= 0) return;

        CompoundTag data = new CompoundTag();
        data.putLong(STORED, node.getNetChrononStored());
        data.putLong(CAPACITY, capacity);
        tag.put(TAG, data);
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon_sync");
    }
}
