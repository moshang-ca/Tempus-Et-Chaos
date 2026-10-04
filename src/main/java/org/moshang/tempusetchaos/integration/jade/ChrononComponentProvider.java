package org.moshang.tempusetchaos.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.TempusEtChaos;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;

public enum ChrononComponentProvider implements IComponentProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.showDetails()) return;
        CompoundTag data = accessor.getServerData().getCompound(ChrononNetDataProvider.TAG);
        long capacity = data.getLong(ChrononNetDataProvider.CAPACITY);
        if (capacity <= 0) return;
        long stored = data.getLong(ChrononNetDataProvider.STORED);
        float ratio = Math.min(1f, stored / (float) capacity);

        IElementHelper helper = IElementHelper.get();
        IDisplayHelper displayHelper = IDisplayHelper.get();
        String cur = displayHelper.humanReadableNumber(stored, "CH", false);
        String max = displayHelper.humanReadableNumber(capacity, "CH", false);
        Component text = Component.translatable("jade.tempusetchaos.chronon", cur, max);
        ProgressStyle style = helper.progressStyle()
                .textColor(0xFFFFFF)
                .overlay(helper.sprite(ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon"), 16, 16));

        tooltip.add(helper.progress(ratio, text, style, BoxStyle.getNestedBox(), true));
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "chronon_display");
    }
}
