package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

/** A single line of text re-read every frame. */
@ParametersAreNonnullByDefault
public class TextElement extends UiElement {
    private final Supplier<Component> text;
    private final int color;
    private final boolean center;

    public TextElement(int x, int y, int width, int height, Supplier<Component> text, int color, boolean center) {
        super(x, y, width, height);
        this.text = text;
        this.color = color;
        this.center = center;
    }

    public TextElement(int x, int y, Supplier<Component> text, int color) {
        super(x, y, 0, 0);
        this.text = text;
        this.color = color;
        this.center = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (center)
            graphics.drawCenteredString(Minecraft.getInstance().font, text.get(), width / 2, height / 2, color);
        else
            graphics.drawString(Minecraft.getInstance().font, text.get(), 0, 0, color, false);
    }

    @Override
    public boolean takesFocus() {
        return false;
    }
}
