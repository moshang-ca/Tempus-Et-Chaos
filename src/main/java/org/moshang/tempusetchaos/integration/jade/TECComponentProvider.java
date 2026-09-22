package org.moshang.tempusetchaos.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.registry.TECUtilities;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

public enum TECComponentProvider implements IComponentProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(TECServerDataProvider.TAG)) return;

        CompoundTag data = serverData.getCompound(TECServerDataProvider.TAG);
        int stored = data.getInt(TECServerDataProvider.STORED);
        int capacity = data.getInt(TECServerDataProvider.CAPACITY);
        if (capacity <= 0) return;

        IElementHelper helper = IElementHelper.get();
        Fluid gas = TECUtilities.GAS_ENTROPY_SOURCE.get();
        Component name = new FluidStack(gas, 1).getHoverName();
        Component text = Component.translatable(
                "jade.tempusetchaos.entropy_amount", name, stored, capacity);
        float ratio = Math.min(1f, stored / (float) capacity);

        ProgressStyle pStyle = helper.progressStyle()
                        .textColor(0xFFFFFF)
                        .overlay(helper.sprite(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "gas_entropy"), 22, 16));
        // tooltip.add(helper.fluid(JadeFluidObject.of(gas, stored)));
        tooltip.add(helper.progress(ratio, text, pStyle, BoxStyle.getNestedBox(), true));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "tec_display");
    }
}
