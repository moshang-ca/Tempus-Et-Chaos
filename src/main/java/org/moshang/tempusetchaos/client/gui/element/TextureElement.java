package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** An element show the given texture, no any special usage. */
public class TextureElement extends UiElement{
    protected final ResourceLocation texture;

    public TextureElement(int x, int y, int width, int height, ResourceLocation texture) {
        super(x, y, width, height);
        this.texture = texture;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(texture, 0, 0, getWidth(), getHeight(), 0f, 0f, getWidth(), getHeight(), getWidth(), getHeight());
    }
}
