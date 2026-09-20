package org.moshang.tempusetchaos.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.registry.TECCapabilities;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public class TECServerDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final TECServerDataProvider INSTANCE = new TECServerDataProvider();

    public static final String TAG = "entropy_fluid";
    public static final String STORED = "stored";
    public static final String CAPACITY = "capacity";

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        IFluidHandler handler = accessor.getLevel()
                .getCapability(TECCapabilities.FLUID_ENTROPY, accessor.getPosition(), null);
        if (handler == null || handler.getTanks() <= 0) return;

        CompoundTag data = new CompoundTag();
        data.putInt(STORED, handler.getFluidInTank(0).getAmount());
        data.putInt(CAPACITY, handler.getTankCapacity(0));
        tag.put(TAG, data);
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "tec_server_data");
    }
}
