package org.moshang.tempusetchaos.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

/** A slot frame with an optional item in it, the position being the item's top left corner like a vanilla {@code Slot}. */
@ParametersAreNonnullByDefault
public class SlotElement extends UiElement {
    private final Supplier<ItemStack> stack;
    private final Supplier<List<Component>> tooltip;

    public SlotElement(int x, int y, Supplier<ItemStack> stack, Supplier<List<Component>> tooltip) {
        this.x = x;
        this.y = y;
        this.width = 18;
        this.height = 18;
        this.stack = stack;
        this.tooltip = tooltip;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(UiTextures.SLOT, -1, -1, 18, 18, 0f, 0f, 18, 18, 18, 18);
        ItemStack item = stack.get();
        if (!item.isEmpty()) graphics.renderItem(item, 0, 0);
    }

    @Override
    public List<Component> getTooltip() {
        return tooltip.get();
    }
}
