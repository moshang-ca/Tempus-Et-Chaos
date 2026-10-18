package org.moshang.tempusetchaos.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.element.ProgressElement;
import org.moshang.tempusetchaos.client.gui.element.ResourceGaugeElement;
import org.moshang.tempusetchaos.client.gui.element.TextElement;
import org.moshang.tempusetchaos.client.gui.window.SimpleContainerWindow;
import org.moshang.tempusetchaos.menu.MenuExtractor;
import org.moshang.tempusetchaos.network.ChrononNetworkSyncPayload;
import org.moshang.tempusetchaos.util.TECLocalizations;

import java.util.ArrayList;
import java.util.List;

public class ScreenExtractor extends ContainerWindowScreen<MenuExtractor> {
    public ScreenExtractor(MenuExtractor menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void populate() {
        windows.open(new MainWindow());
    }

    private class MainWindow extends SimpleContainerWindow<MenuExtractor> {
        public MainWindow() {
            super(ScreenExtractor.this.menu, 176, 166, UiTextures.BASE_INVENTORY);

            elements.add(new ResourceGaugeElement(128, 14, 10, 60, UiTextures.CHANNEL,
                    () -> ChrononNetworkSyncPayload.ClientCache.getRatio(menu.getUuid()), -1,
                    UiTextures.sprite("chronon"), this::getTooltip))
                    .add(new ProgressElement(63, 35, 54, 18, UiTextures.PROGRESS, menu.getBlockEntity()::getProgressRatio,
                            9, 54, 36))
                    .add(new TextElement(63, 19, 54, 18,
                            () -> Component.translatable(TECLocalizations.CHRONON_RATE,
                                    TECLocalizations.formatCompact(menu.getBlockEntity().getChrononRate(), false, "CH", true)), 0xFF10EE10, true));
        }

        private List<Component> getTooltip() {
            List<Component> tooltip = new ArrayList<>();
            ChrononNetworkSyncPayload.ClientCache cache = ChrononNetworkSyncPayload.ClientCache.get(menu.getUuid());
            tooltip.add(Component.translatable(TECLocalizations.CHRONON, TECLocalizations.formatCompact(cache.stored(), false, "CH"),
                    TECLocalizations.formatCompact(cache.capacity(), false, "CH")));
            return tooltip;
        }


        @Override
        protected void renderSlotBg(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
            if (slot.index < menu.getMachineSlots())
                graphics.blitSprite(UiTextures.SLOT, slot.x - 1, slot.y - 1, SLOT_FRAME, SLOT_FRAME);
        }
    }
}
