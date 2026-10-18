package org.moshang.tempusetchaos.client.gui.element;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A list the player scrolls and picks from: the rows live in the viewport, the controls in a strip below it, and the
 * selection belongs to the list, so it can only ever point at one row.
 */
@ParametersAreNonnullByDefault
@Accessors(fluent = true, chain = true)
public class ListElement extends UiElement implements IScrollable {
    private static final int CONTROL_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int TRACK_COLOR = 0x40000000;
    private static final int THUMB_COLOR = 0x80FFFFFF;

    @Accessors(fluent = false) @Getter
    private final Scroll scroll = new Scroll();
    @Setter @Getter
    private int selected = -1;

    private final int rowHeight;
    /** The rows on screen, each clipped to the row area above the control strip. Only the visible ones exist. */
    private final UiHost<ListRowElement> rowHost = new UiHost<>() {
        /** Folded over by the element's own host, which is what lets a row clip in screen space. */
        @Override
        public AnimProps.Host toScreen(double x, double y) {
            return owner == null ? super.toScreen(x, y) : owner.toScreen(x + getX(), y + getY());
        }
    };
    /** The host holding this element, so the row host can map its own space out. */
    @Nullable
    private UiHost<?> owner;
    /** The strip the controls live in. What is added there is laid out left to right, vertically centred. */
    @Getter
    private final UiHost<UiElement> controls = new UiHost<>();

    @Setter
    private Supplier<Integer> size = () -> 0;
    @Setter
    private IntFunction<ItemStack> icons = index -> ItemStack.EMPTY;
    @Setter
    private IntFunction<Component> labels = index -> Component.empty();
    @Nullable
    @Setter
    private IntConsumer onPick;

    public ListElement(int x, int y, int width, int height, int rowHeight) {
        super(x, y, width, height);
        this.rowHeight = rowHeight;
    }

    /** The row area above the control strip, which is as tall as the tallest control in it. */
    public int rowsHeight() {
        return Math.max(0, getHeight() - controlsHeight());
    }

    /** The strip is measured from what is in it, so no control can be taller than the strip drawn around it. */
    private int controlsHeight() {
        int tallest = 0;
        for (UiElement control : controls.getNodes()) {
            tallest = Math.max(tallest, control.getHeight());
        }
        return tallest == 0 ? 0 : tallest + 2 * CONTROL_GAP;
    }

    @Override
    public void onAttached(UiHost<?> host) {
        owner = host;
    }

    /**
     * The row area expressed in the space of the row that starts at {@code rowY}: a row is cut there and at its own
     * width, so a label wider than the row can never spill over the scrollbar or the selected band. Null while the
     * screen mapping is missing.
     */
    @Nullable
    private Rect2i rowBand(int rowY, int width) {
        if (owner == null) return null;
        return new Rect2i(0, -rowY, width, rowsHeight());
    }

    /**
     * The whole box, the control strip included.
     */
    @Override
    public Rect2i viewport() {
        return new Rect2i(0, 0, getWidth(), getHeight());
    }

    @Override
    public void init(Minecraft mc, int hostWidth, int hostHeight) {
        rowHost.init(getWidth(), rowsHeight());
        controls.init(getWidth(), controlsHeight());
        layoutControls();
    }

    @Override
    public void tick() {
        rowHost.tick();
        controls.tick();
    }

    /**
     * The list's own scroll and the two hosts underneath it. The rows are also reconciled here rather than in
     * {@link #tick()}: the data can change between a tick and the frame that draws it, and a row left behind by that
     * would read an entry the caller no longer has.
     */
    @Override
    public void updateAnimation() {
        anims().tick();
        int total = Math.max(0, size.get());
        scroll.setContentHeight(total * rowHeight);
        scroll.advance(getMaxScroll());
        syncRows(total);
        layoutControls();
        rowHost.updateAnimations();
        controls.updateAnimations();
    }

    /**
     * Keeps exactly the rows the row area can hold and points each of them at the entry it currently stands for: the
     * row count follows the viewport, never the data, so a list of a thousand entries still moves a handful of rows.
     */
    private void syncRows(int total) {
        List<ListRowElement> rows = rowHost.getNodes();
        int wanted = Math.min(visibleRows(), total);
        while (rows.size() < wanted) rowHost.add(new ListRowElement(this, rowWidth(), rowHeight));
        while (rows.size() > wanted) rowHost.remove(rows.getLast());
        // the selection is a position, so it stops meaning anything once the data is gone
        if (selected >= total) selected = -1;

        int offset = scroll.getOffset();
        int first = offset / rowHeight;
        for (int i = 0; i < rows.size(); i++) {
            int index = first + i;
            rows.get(i).show(index < total ? index : -1, SCROLLBAR_WIDTH, index * rowHeight - offset);
        }
    }

    /** How many rows the row area can hold, counting the one crossing its bottom edge. */
    private int visibleRows() {
        int band = rowsHeight();
        if (band <= 0) return 0;
        int offset = scroll.getOffset();
        return (offset + band - 1) / rowHeight - offset / rowHeight + 1;
    }

    private int rowWidth() {
        return getWidth() - 2 * SCROLLBAR_WIDTH;
    }

    private void layoutControls() {
        int strip = controlsHeight();
        int x = 0;
        for (UiElement control : controls.getNodes()) {
            control.setPosition(x, (strip - control.getHeight()) / 2);
            x += control.getWidth() + CONTROL_GAP;
        }
    }

    private void pick(int index) {
        if (selected == index) return;
        selected = index;
        if (onPick != null) onPick.accept(index);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        rowHost.render(graphics, mouseX, mouseY, partialTick);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, rowsHeight(), 0f);
        controls.render(graphics, mouseX, mouseY - rowsHeight(), partialTick);
        graphics.pose().popPose();
        renderScrollbar(graphics);
    }

    /** Drawn by the list itself: the scroll state made visible. */
    private void renderScrollbar(GuiGraphics graphics) {
        int max = getMaxScroll();
        if (max <= 0) return;

        int trackHeight = rowsHeight();
        int x = getWidth() - SCROLLBAR_WIDTH;
        UiHost.clearContentDepth(graphics);
        graphics.fill(x, 0, x + SCROLLBAR_WIDTH, trackHeight, TRACK_COLOR);
        int thumbHeight = Math.clamp((long) trackHeight * trackHeight / getScroll().getContentHeight(), 8, trackHeight);
        int thumbY = (trackHeight - thumbHeight) * getScroll().getOffset() / max;
        graphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, THUMB_COLOR);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        rowHost.mouseMoved(mouseX, mouseY);
        controls.mouseMoved(mouseX, mouseY - rowsHeight());
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= rowsHeight()) return controls.isMouseClicked(mouseX, mouseY - rowsHeight(), button);
        return rowHost.isMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        if (mouseY >= rowsHeight()) return controls.isMouseReleased(mouseX, mouseY - rowsHeight(), button);
        return rowHost.isMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseY >= rowsHeight()) return controls.isMouseDragged(mouseX, mouseY - rowsHeight(), button, dragX, dragY);
        return rowHost.isMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return rowHost.keyPressed(keyCode, scanCode, modifiers) || controls.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return rowHost.keyReleased(keyCode, scanCode, modifiers) || controls.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return rowHost.charTyped(codePoint, modifiers) || controls.charTyped(codePoint, modifiers);
    }

    @Override
    public List<Component> getTooltip() {
        UiElement hovered = rowHost.getHovered() != null ? rowHost.getHovered() : controls.getHovered();
        return hovered == null ? List.of() : hovered.getTooltip();
    }

    @Override
    public boolean takesFocus() {
        return false;
    }

    /**
     * One row of a list: an optional icon, a label and the selected marker. It is a leaf the list reuses, so it holds no
     * entry of its own: the index it was last pointed at is what it draws, and it reads everything else back from the
     * list.
     */
    protected static class ListRowElement extends UiElement {
        private static final int ICON_SIZE = 16;
        private static final int PADDING = 2;
        private static final int TEXT_COLOR = 0xFF101010;
        private static final int SELECTED_BACKGROUND = 0xFF808080;
        private static final int HOVER_BACKGROUND = 0x40FFFFFF;

        private final List<Component> tooltip = new ArrayList<>();
        private final ListElement list;
        /** The entry this row stands for right now, -1 while it is a spare slot. */
        private int index = -1;

        ListRowElement(ListElement list, int width, int height) {
            super(0, 0, width, height);
            this.list = list;
        }

        /** Points this row at an entry and puts it where that entry is drawn. */
        private void show(int index, int x, int y) {
            boolean changed = this.index != index;
            this.index = index;
            setPosition(x, y);
            visible(index >= 0);
            if (!changed) return;
            if (index < 0) tooltip.clear();
            else if (isHovered()) onHoverChanged(true);
        }

        /**
         * The row box: the row area vertically, the row's own width horizontally.
         */
        @Nullable
        @Override
        public Rect2i viewport() {
            return list.rowBand(getY(), getWidth());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (list.selected == index) graphics.fill(0, 0, getWidth(), getHeight(), SELECTED_BACKGROUND);
            else if (isHovered()) graphics.fill(0, 0, getWidth(), getHeight(), HOVER_BACKGROUND);

            int textX = PADDING;
            ItemStack stack = list.icons.apply(index);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, PADDING, (getHeight() - ICON_SIZE) / 2);
                textX += ICON_SIZE + PADDING;
            }
            graphics.drawString(Minecraft.getInstance().font, list.labels.apply(index), textX, (getHeight() - 8) / 2, TEXT_COLOR, false);
        }

        @Override
        public boolean isMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || index < 0) return false;
            list.pick(index);
            return true;
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            tooltip.clear();
            if (!hovered) return;
            Component label = list.labels.apply(index);
            // the label is cut at the row's edge, so the tooltip is what has to carry the rest of it
            int available = getWidth() - PADDING - (list.icons.apply(index).isEmpty() ? 0 : ICON_SIZE + PADDING);
            if (Minecraft.getInstance().font.width(label) > available) tooltip.add(label);
        }

        @Override
        public List<Component> getTooltip() {
            return tooltip;
        }

        @Override
        public boolean takesFocus() {
            return false;
        }
    }
}
