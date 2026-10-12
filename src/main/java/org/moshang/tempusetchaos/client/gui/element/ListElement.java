package org.moshang.tempusetchaos.client.gui.element;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.moshang.tempusetchaos.client.gui.IScrollable;
import org.moshang.tempusetchaos.client.gui.UiHost;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
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
    private static final int CONTROLS_HEIGHT = 14;
    private static final int CONTROL_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int TRACK_COLOR = 0x40000000;
    private static final int THUMB_COLOR = 0x80FFFFFF;

    @Accessors(fluent = false) @Getter
    private final Scroll scroll = new Scroll();
    @Setter @Getter
    private int selected = -1;

    private final int rowHeight;
    /** The rows, each clipped to the row area above the control strip. */
    private final UiHost<UiElement> rowHost = new UiHost<>() {
        /** Folded over by the element's own host, which is what lets a row clip in screen space. */
        @Override
        public AnimProps.Local toScreen(double x, double y) {
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

    /** The row area above the control strip. */
    public int rowsHeight() {
        return Math.max(0, getHeight() - CONTROLS_HEIGHT);
    }

    @Override
    public void onAttached(UiHost<?> host) {
        owner = host;
    }

    /**
     * The row area expressed in the space of the row that starts at {@code rowY}, so a row crossing its boundary is cut
     * there. Null for a row fully inside the area, and for any row while the screen mapping is missing.
     */
    @Nullable
    private Rect2i rowBand(int rowY, int rowSize, int width) {
        if (owner == null) return null;
        int band = rowsHeight();
        if (rowY >= 0 && rowY + rowSize <= band) return null;
        return new Rect2i(0, -rowY, width, band);
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
        controls.init(getWidth(), CONTROLS_HEIGHT);
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
        syncRows();
        scroll.setContentHeight(rowHost.getNodes().size() * rowHeight);
        scroll.advance(getMaxScroll());
        layoutControls();
        rowHost.updateAnimations();
        controls.updateAnimations();
    }

    /** The rows follow the data, so the host is only ever asked for as many as there are. */
    private void syncRows() {
        List<UiElement> rows = rowHost.getNodes();
        int wanted = Math.max(0, size.get());
        while (rows.size() < wanted) rowHost.add(newRow(rows.size()));
        while (rows.size() > wanted) rowHost.remove(rows.getLast());
        // the selection is a position, so it stops meaning anything once the rows are gone
        if (selected >= rows.size()) selected = -1;
        layoutRows();
    }

    private ListRowElement newRow(int index) {
        return new ListRowElement(this, index, getWidth() - 2 * SCROLLBAR_WIDTH, rowHeight,
                () -> icons.apply(index), () -> labels.apply(index),
                () -> selected == index, this::pick);
    }

    /** The scroll is an offset on the rows themselves, so the host keeps testing them where they really are. */
    private void layoutRows() {
        List<UiElement> rows = rowHost.getNodes();
        for (int i = 0; i < rows.size(); i++) {
            UiElement row = rows.get(i);
            int y = i * rowHeight - scroll.getOffset();
            row.setPosition((getWidth() - row.getWidth()) / 2, y);
            // hidden, not just off screen: a row that is not in the row area is neither drawn nor hit
            row.setVisible(y + rowHeight > 0 && y < rowsHeight());
        }
    }

    private void layoutControls() {
        int x = 0;
        for (UiElement control : controls.getNodes()) {
            control.setPosition(x, (CONTROLS_HEIGHT - control.getHeight()) / 2);
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
     * One row of a list: an optional icon, a label and the selected marker. It is a leaf, the list owns the row count,
     * the order and which row is selected; the row only reads them back and reports a click.
     */
    protected static class ListRowElement extends UiElement {
        private static final int ICON_SIZE = 16;
        private static final int PADDING = 2;
        private static final int TEXT_COLOR = 0xFF101010;
        private static final int SELECTED_BACKGROUND = 0xFF808080;
        private static final int HOVER_BACKGROUND = 0x40FFFFFF;

        private final List<Component> tooltip = new ArrayList<>();
        private final ListElement list;
        private final Supplier<ItemStack> icon;
        private final Supplier<Component> label;
        private final BooleanSupplier selected;
        private final IntConsumer onPick;
        private final int index;

        public ListRowElement(ListElement list, int index, int width, int height,
                              Supplier<ItemStack> icon, Supplier<Component> label,
                              BooleanSupplier selected, IntConsumer onPick) {
            super(0, 0, width, height);
            this.list = list;
            this.index = index;
            this.icon = icon;
            this.label = label;
            this.selected = selected;
            this.onPick = onPick;
        }

        /**
         * The row area expressed in this row's own space, so a row crossing its boundary is cut there.
         */
        @Nullable
        @Override
        public Rect2i viewport() {
            return list.rowBand(getY(), getHeight(), getWidth());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (selected.getAsBoolean()) graphics.fill(0, 0, getWidth(), getHeight(), SELECTED_BACKGROUND);
            else if (isHovered()) graphics.fill(0, 0, getWidth(), getHeight(), HOVER_BACKGROUND);

            int textX = PADDING;
            ItemStack stack = icon.get();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, PADDING, (getHeight() - ICON_SIZE) / 2);
                textX += ICON_SIZE + PADDING;
            }
            graphics.drawString(Minecraft.getInstance().font, label.get(), textX, (getHeight() - 8) / 2, TEXT_COLOR, false);
        }

        @Override
        public boolean isMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            onPick.accept(index);
            return true;
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            if (hovered && Minecraft.getInstance().font.width(label.get()) > width) {
                tooltip.add(label.get());
            } else {
                tooltip.clear();
            }
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
