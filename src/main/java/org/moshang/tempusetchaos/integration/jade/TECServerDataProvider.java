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

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        IFluidHandler fluidHandler = blockAccessor.getLevel()
                .getCapability(TECCapabilities.FLUID_ENTROPY, blockAccessor.getPosition(), null);
        if (fluidHandler != null) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString("fluid", fluidHandler.getFluidInTank(0).getFluidHolder().getRegisteredName());
            fluidTag.putInt("stored", fluidHandler.getFluidInTank(0).getAmount());
            fluidTag.putInt("capacity", fluidHandler.getTankCapacity(0));

            compoundTag.put("entropy_fluid", fluidTag);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "tec_server_data");
    }
}
