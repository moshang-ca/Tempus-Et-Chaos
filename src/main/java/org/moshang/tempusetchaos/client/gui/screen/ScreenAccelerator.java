package org.moshang.tempusetchaos.client.gui.screen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.moshang.tempusetchaos.client.gui.ContainerWindowScreen;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.*;
import org.moshang.tempusetchaos.client.gui.window.SimpleContainerWindow;
import org.moshang.tempusetchaos.menu.MenuAccelerator;

public class ScreenAccelerator extends ContainerWindowScreen<MenuAccelerator> {
    public ScreenAccelerator(MenuAccelerator menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void populate() {
        windows.open(new MainWindow());
    }

    private class MainWindow extends SimpleContainerWindow<MenuAccelerator> {
        private static final Cue SPIN = new Cue(0, 150, Easing.EASE_IN_EXPO);
        private static final Cue OPEN = Cue.startingAt(SPIN, 100, 200, Easing.EASE_IN_EXPO);

        public MainWindow() {
            super(ScreenAccelerator.this.menu, 176, 166, UiTextures.BASE_INVENTORY, 176, 166);
//            elements.add(new ResourceGaugeElement(12, 17, 10, 54, UiTextures.CHANNEL, ))
//                    .add(new )
//                    .add(new TextElement(0, 0, 20, 10, () -> Component.literal(String.valueOf(ChrononNetworkSyncPayload.ClientCache.get(menu.getUuid()).stored())), 0xFF0000FF));
            anims().add(Anims.timeline(AnimEvent.ADDED, SPIN, SPIN.track(0, 1), AnimProps.Writer.SCALE_X))
                    .add(Anims.timeline(AnimEvent.ADDED, OPEN, OPEN.track(0.01f, 1), AnimProps.Writer.SCALE_Y));
        }
    }
}
