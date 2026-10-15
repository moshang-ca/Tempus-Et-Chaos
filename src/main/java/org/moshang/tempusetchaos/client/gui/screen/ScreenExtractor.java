package org.moshang.tempusetchaos.client.gui.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.IItemHandler;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.element.SlotElement;
import org.moshang.tempusetchaos.client.gui.window.SimpleContainerWindow;
import org.moshang.tempusetchaos.menu.MenuExtractor;

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

            IItemHandler itemHandler = menu.getBlockEntity().getInputHandler();
            for (int i = 0; i < itemHandler.getSlots(); ++i) {
                final int idx = i;
                elements.add(SlotElement.ofNoTooltip(33 + i * 18, 33,
                        () -> itemHandler.getStackInSlot(idx)));
            }
        }
    }
}
