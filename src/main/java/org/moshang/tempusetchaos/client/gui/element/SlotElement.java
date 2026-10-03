package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.UiTextures;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

/**
 * A slot frame with an optional item in it.
 * Its bounds are the 16x16 item area, the same box vanilla hit tests, the frame pokes one pixel out of it.
 */
@ParametersAreNonnullByDefault
public class SlotElement extends UiElement {
    private static final int ITEM_SIZE = 16;

    protected final Supplier<ItemStack> stack;
    private final Supplier<List<Component>> tooltip;

    public SlotElement(int x, int y, Supplier<ItemStack> stack, Supplier<List<Component>> tooltip) {
        super(x, y, ITEM_SIZE, ITEM_SIZE);
        this.stack = stack;
        this.tooltip = tooltip;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(UiTextures.SLOT, -1, -1, 18, 18, 0f, 0f, 18, 18, 18, 18);
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
}
