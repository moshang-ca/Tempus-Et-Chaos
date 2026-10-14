package org.moshang.tempusetchaos.client.gui.element;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.UiTextures;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;

/**
 * A group of phantom slots filled as one area: a drag from outside the screen lands in the first free cell, while a
 * click works the single cell under the cursor. Nothing is stored here: the index based reader and writer own the
 * content, the cells only read it back and draw it.
 */
// TODO: Add scrollable feature.
@ParametersAreNonnullByDefault
@Accessors(fluent = true, chain = true)
public class PhantomAreaElement extends UiElement implements GhostSlot {
    private static final int CELL = 18;

    private final int columns;
    private final int rows;
    private final IntFunction<ItemStack> content;
    private final ObjIntConsumer<ItemStack> setter;
    private final List<SlotElement> cells = new ArrayList<>();

    @Setter
    private boolean canOverflow = false;
    @Setter
    private Predicate<ItemStack> filter = stack -> true;
    @Setter
    private Function<Integer, List<Component>> tooltips;
    private int hoveredCell = -1;

    @Setter
    private ResourceLocation texture = UiTextures.INPUT_FIELD;

    public PhantomAreaElement(int x, int y, int columns, int rows, IntFunction<ItemStack> content, ObjIntConsumer<ItemStack> setter) {
        super(x, y, columns * CELL, rows * CELL);
        this.columns = columns;
        this.rows = rows;
        this.content = content;
        this.setter = setter;
        this.tooltips = index -> {
            ItemStack stack = content.apply(index);
            return stack.isEmpty() ? List.of() : List.of(stack.getHoverName());
        };

        int count = columns > 0 && rows > 0 ? columns * rows : 0;
        for (int i = 0; i < count; i++) {
            int index = i;
            cells.add(new SlotElement(index % columns * CELL + 1, index / columns * CELL + 1,
                    () -> content.apply(index), () -> tooltips.apply(index)).noTexture());
        }
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return filter.test(stack) && (firstFree() >= 0 || canOverflow);
    }

    @Override
    public void accept(ItemStack stack) {
        int index = firstFree();
        if (index >= 0 || canOverflow) setter.accept(stack.copy(), index);
    }

    private int firstFree() {
        for (int i = 0; i < cells.size(); i++) {
            if (content.apply(i).isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        int index = indexAt(mouseX, mouseY);
        if (index < 0) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;

        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) setter.accept(ItemStack.EMPTY, index);
        else if (filter.test(carried)) setter.accept(carried.copy(), index);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(texture, 0, 0, width, height);
        hoveredCell = indexAt(mouseX, mouseY);
        for (int i = 0; i < cells.size(); i++) {
            SlotElement cell = cells.get(i);
            boolean hovered = i == hoveredCell;
            if (cell.isHovered() != hovered) cell.onHoverChanged(hovered);
            graphics.pose().pushPose();
            graphics.pose().translate(cell.getX(), cell.getY(), 0f);
            cell.render(graphics, 0, 0, partialTick);
            graphics.pose().popPose();
        }
    }

    @Override
    public void renderForeground(GuiGraphics graphics, AnimProps.Host mouse, float partialTick) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x38FFFFFF);
    }

    @Override
    public List<Component> getTooltip() {
        return hoveredCell < 0 ? List.of() : cells.get(hoveredCell).getTooltip();
    }

    @Override
    public boolean takesFocus() {
        return false;
    }

    private int indexAt(double mouseX, double mouseY) {
        if (mouseX < 0 || mouseY < 0) return -1;
        int column = (int) (mouseX / CELL);
        int row = (int) (mouseY / CELL);
        return column < columns && row < rows ? row * columns + column : -1;
    }
}
