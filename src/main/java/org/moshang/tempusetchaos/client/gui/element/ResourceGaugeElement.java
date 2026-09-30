package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.texture = texture;
        this.ratio = ratio;
        this.fillColor = fillColor;
        this.resourceSprite = resourceSprite;
        this.tooltip = tooltip;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(texture, 0, 0, getWidth(), getHeight(), 0f, 0f, getWidth(), getHeight(), getWidth(), getHeight());
        int fill = Math.round((getHeight() - 2) * Math.clamp(ratio.get(), 0f, 1f));
        if (fill <= 0) return;
        if (resourceSprite == null) {
            graphics.fill(1, getHeight() - 1 - fill, getWidth() - 1, getHeight() - 1, fillColor);
        } else {
            graphics.blitSprite(resourceSprite, 1, getHeight() - 1 - fill, getWidth() - 1, getHeight() - 1);
        }
    }

    @Override
    public List<Component> getTooltip() {
        return tooltip.get();
    }
}
