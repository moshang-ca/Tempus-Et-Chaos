package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.client.gui.UiTextures;

import java.util.function.Supplier;

/**
 * A bar of any width: the sprite's rectangular part stretches, the head keeps its {@code headWidth} pixels.
 * The sprite holds two frames of the same size, the empty track on top and the filled one below, and its
 * rectangular part has to be uniform along x, so stretching it always looks the same.
 */
public class ProgressElement extends UiElement {
    private final ResourceLocation sprite;
    private final int texWidth;
    private final int texHeight;
    private final int headWidth;
    private final Supplier<Float> ratio;

    public ProgressElement(int x, int y, int width, int height,
                           ResourceLocation sprite, Supplier<Float> ratio,
                           int headWidth, int texWidth, int texHeight) {
        super(x, y, width, height);
        this.sprite = sprite;
        this.ratio = ratio;
        this.headWidth = headWidth;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(graphics, 0, 0, getWidth(), getHeight(), sprite, headWidth, texWidth, texHeight,
                Math.round(Math.clamp(ratio.get(), 0f, 1f) * getWidth()));
    }

    /** The whole arrow filled, for a caller that has no progress to show; only the two ends of the fill exist here. */
    public static void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height,
                            ResourceLocation sprite, int headWidth, int texWidth, int texHeight) {
        draw(graphics, x, y, width, height, sprite, headWidth, texWidth, texHeight, width);
    }

    private static void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height,
                             ResourceLocation sprite, int headWidth, int texWidth, int texHeight, int filledWidth) {
        int frameHeight = texHeight / 2;
        int bar = texWidth - headWidth;
        int body = Math.max(0, width - headWidth);

        slice(graphics, x, y, height, sprite, texWidth, texHeight, frameHeight, 0, bar, 0, 0, body);
        slice(graphics, x, y, height, sprite, texWidth, texHeight, frameHeight, bar, headWidth, 0, body, headWidth);

        if (filledWidth <= 0) return;
        int filledBody = Math.min(filledWidth, body);
        if (filledBody > 0) {
            slice(graphics, x, y, height, sprite, texWidth, texHeight, frameHeight, 0, bar, frameHeight, 0, filledBody);
        }
        if (filledWidth > body) {
            slice(graphics, x, y, height, sprite, texWidth, texHeight, frameHeight, bar, headWidth, frameHeight, body, filledWidth - body);
        }
    }

    private static void slice(GuiGraphics graphics, int x, int y, int height, ResourceLocation sprite,
                              int texWidth, int texHeight, int frameHeight,
                              int srcX, int srcWidth, int frameV, int dstX, int dstWidth) {
        graphics.blit(UiTextures.spriteFile(sprite), x + dstX, y, dstWidth, height,
                srcX, frameV, srcWidth, frameHeight, texWidth, texHeight);
    }

    @Override
    public boolean takesFocus() {
        return false;
    }

    @Override
    public boolean takesPointer() {
        return false;
    }
}
