package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/** A single line of text re-read every frame, so it can follow synced menu data. */
@ParametersAreNonnullByDefault
public class TextElement extends UiElement {
    private final Supplier<Component> text;
    private final int color;

    public TextElement(int x, int y, int width, int height, Supplier<Component> text, int color) {
        super(x, y, width, height);
        this.text = text;
        this.color = color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(Minecraft.getInstance().font, text.get(), 0, 0, color, false);
    }

    @Override
    public boolean takesFocus() {
        return false;
    }
}
