package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.moshang.tempusetchaos.client.gui.UiTextures;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

/** A vertical gauge: the texture is the channel, the fill grows from the bottom. */
@ParametersAreNonnullByDefault
public class ResourceGaugeElement extends UiElement {
    private final ResourceLocation texture;
    private final Supplier<Float> ratio;
    private final int fillColor;
    @Nullable
    private final ResourceLocation resourceSprite;
    private final Supplier<List<Component>> tooltip;

    public static ResourceGaugeElement ofNoSprite(int x, int y, int width, int height, ResourceLocation texture,
                                                  Supplier<Float> ratio, int fillColor, Supplier<List<Component>> tooltip) {
        return new ResourceGaugeElement(x, y, width, height, texture, ratio, fillColor, null, tooltip);
    }

    public ResourceGaugeElement(int x, int y, int width, int height, ResourceLocation texture,
                                Supplier<Float> ratio, int fillColor, @Nullable ResourceLocation resourceSprite, Supplier<List<Component>> tooltip) {
        super(x, y, width, height);
        this.texture = texture;
        this.ratio = ratio;
        this.fillColor = fillColor;
        this.resourceSprite = resourceSprite;
        this.tooltip = tooltip;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(graphics, 0, 0, getWidth(), getHeight(), texture, ratio.get(), resourceSprite, fillColor);
    }

    public static void draw(GuiGraphics graphics, int x, int y, int width, int height, ResourceLocation texture,
                            float ratio, @Nullable ResourceLocation resourceSprite, int fillColor) {
        UiTextures.blitNineSliced(graphics, texture, x, y, width, height, false);
        int fill = Math.round((height - 2) * Math.clamp(ratio, 0f, 1f));
        if (fill <= 0) return;
        if (resourceSprite == null) {
            graphics.fill(x + 1, y + height - 1 - fill, x + width - 1, y + height - 1, fillColor);
        } else {
            int innerWidth = width - 2;
            int innerHeight = height - 2;

            UiTextures.blitTranslucentSprite(graphics, resourceSprite, innerWidth, innerHeight,
                    0, innerHeight - fill, x + 1, y + height - 1 - fill, innerWidth, fill);
        }
    }

    @Override
    public List<Component> getTooltip() {
        return tooltip.get();
    }

    @Override
    public boolean takesFocus() {
        return false;
    }
}
