package org.moshang.tempusetchaos.integration.jei.gui;

import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.AbstractWindowScreen;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WindowScreenProperties implements IScreenHandler<AbstractWindowScreen> {
    @Override
    @Nullable
    public IGuiProperties apply(AbstractWindowScreen screen) {
        if (screen.width <= 1 || screen.height <= 1) return null;

        Rect2i area = screen.windowArea();
        if (area == null) return null;

        int left = Math.max(0, area.getX());
        int top = Math.max(0, area.getY());
        int right = Math.min(screen.width, area.getX() + area.getWidth());
        int bottom = Math.min(screen.height, area.getY() + area.getHeight());
        if (right <= left || bottom <= top) return null;

        return new Properties(screen.getClass(), left, top, right - left, bottom - top, screen.width, screen.height);
    }

    private record Properties(Class<? extends Screen> screenClass, int guiLeft, int guiTop, int guiXSize, int guiYSize,
                              int screenWidth, int screenHeight) implements IGuiProperties {
    }
}
