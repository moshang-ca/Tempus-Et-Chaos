package org.moshang.tempusetchaos.integration.jade;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.TempusEtChaos;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.impl.ui.ElementHelper;

public class TECComponentProvider implements IComponentProvider<BlockAccessor> {
    public static final TECComponentProvider INSTANCE = new TECComponentProvider();

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor accessor, IPluginConfig iPluginConfig) {
        CompoundTag data = accessor.getServerData();
        if (data.contains("entropy_fluid")) {
            CompoundTag fluidTag = data.getCompound("entropy_fluid");
            int stored = fluidTag.getInt("stored");
            int capacity = fluidTag.getInt("capacity");

            JadeFluidObject fluidObject = JadeFluidObject.of(
                    BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidTag.getString("fluid"))),
                    stored);

            iTooltip.append(ElementHelper.INSTANCE.fluid(fluidObject));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "tec_display");
    }
}
