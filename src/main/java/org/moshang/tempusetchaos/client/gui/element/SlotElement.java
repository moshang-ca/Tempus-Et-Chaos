package org.moshang.tempusetchaos.client.gui.element;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.UiTextures;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class SlotElement extends UiElement {
    private static final int ITEM_SIZE = 16;

    protected final Supplier<ItemStack> stack;
    private final Supplier<List<Component>> tooltip;

    @Nullable
    @Accessors(fluent = true, chain = true) @Setter
    private ResourceLocation texture = UiTextures.SLOT;

    public static SlotElement ofNoTooltip(int x, int y, Supplier<ItemStack> stack) {
        return new SlotElement(x, y, stack, List::of);
    }

    public SlotElement(int x, int y, Supplier<ItemStack> stack, Supplier<List<Component>> tooltip) {
        super(x, y, ITEM_SIZE, ITEM_SIZE);
        this.stack = stack;
        this.tooltip = tooltip;
    }

    public SlotElement noTexture() {
        this.texture = null;
        return this;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (texture != null)
            graphics.blitSprite(texture, -1, -1, 18, 18);
        ItemStack item = stack.get();
        if (!item.isEmpty()) graphics.renderItem(item, 0, 0);
        if (isHovered()) {
            UiHost.clearContentDepth(graphics);
            graphics.fill(0, 0, ITEM_SIZE, ITEM_SIZE, 0x80FFFFFF);
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
