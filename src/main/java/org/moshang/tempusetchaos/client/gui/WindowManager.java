package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;
import org.moshang.tempusetchaos.client.gui.element.GhostSlot;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class WindowManager extends UiHost<IUiWindow> {
    @Getter
    private final Screen host;

    private boolean populated;
    private boolean closingScreen;

    public WindowManager(Screen host) {
        this.host = host;
    }

    public void open(IUiWindow window) {
        add(window);
    }

    /**
     * Lays this manager out for a screen again: the client calls this on every {@code Screen#init}, which happens on a
     * construction, on a return to the front and on a resize. Only a layout that has no window yet runs the populator;
     * a window that is already open keeps its place and its state, and a later layout just lays the live ones out
     * again.
     */
    public void rebuild(int hostWidth, int hostHeight, Runnable populate) {
        if (populated && !isEmpty()) {
            onResize(hostWidth, hostHeight);
            return;
        }
        init(hostWidth, hostHeight);
        populated = false;
        populate.run();
        populated = true;
        if (isEmpty()) log.warn("{} opened no window, the screen stays empty", host.getClass().getName());
    }

    @Override
    protected void onAllNodesGone() {
        if (!populated || closingScreen || host.isPauseScreen()) return;
        closingScreen = true;
        try {
            host.onClose();
        } finally {
            closingScreen = false;
        }
    }

    /**
     * The box the open windows occupy by layout, in screen coordinates, their animations left out: a window that is
     * still sliding or growing owns its place already, so the answer stays steady while it moves, which is what a
     * screen reports to other mods. Null when nothing is open.
     */
    @Nullable
    public Rect2i bounds() {
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (IUiWindow window : nodes) {
            if (!window.isVisible()) continue;
            left = Math.min(left, window.getX());
            top = Math.min(top, window.getY());
            right = Math.max(right, window.getRight());
            bottom = Math.max(bottom, window.getBottom());
        }
        if (left > right) return null;
        return new Rect2i(left, top, right - left, bottom - top);
    }

    /** Every ghost slot of every window that takes the stack, the order following the window stacking. */
    public List<GhostTarget> ghostSlots(ItemStack stack) {
        List<GhostTarget> targets = new ArrayList<>();
        for (IUiWindow window : nodes) {
            if (!(window instanceof AbstractUiWindow ui) || !isInteractive(window)) continue;
            for (UiElement element : ui.elements().getNodes()) {
                if (!element.isVisible()) continue;
                if (element instanceof GhostSlot slot && slot.accepts(stack) && !isCovered(ui, element)) {
                    targets.add(new GhostTarget(ui, element, slot));
                }
            }
        }
        return targets;
    }

    /**
     * The slot the cursor is over, asked of the window that is on top at that point: what the player can click is what
     * another mod gets to look up. Null when the cursor is over no slot at all.
     */
    @Nullable
    public SlotTarget slotAt(double mouseX, double mouseY) {
        if (!(topmostAt(mouseX, mouseY) instanceof ContainerWindow<?> window)) return null;
        AnimProps.Local local = localPoint(window, mouseX, mouseY);
        Slot slot = window.findSlot(local.x(), local.y());
        return slot == null ? null : new SlotTarget(window, slot);
    }

    /**
     * Whether something that takes the pointer is drawn over this element, which makes it a spot the player cannot
     * really reach. The stacking is read, not measured: a node later in its host's list is painted later and therefore
     * sits on top, so only the entries behind this element have to be looked at.
     */
    private boolean isCovered(AbstractUiWindow window, UiElement element) {
        Rect2i box = screenBox(window, element);
        if (box.getWidth() <= 0 || box.getHeight() <= 0) return false;

        List<UiElement> siblings = window.elements().getNodes();
        for (int i = siblings.indexOf(element) + 1; i < siblings.size(); i++) {
            UiElement other = siblings.get(i);
            if (!other.takesPointer() || !overlaps(screenBox(window, other), box)) continue;
            return true;
        }

        int index = nodes.indexOf(window);
        for (int i = index + 1; i < nodes.size(); i++) {
            IUiWindow above = nodes.get(i);
            if (!above.takesPointer() || !overlaps(windowBox(above), box)) continue;
            return true;
        }
        return false;
    }

    /** The element's box in screen coordinates, with both its own transform and the window's applied. */
    private Rect2i screenBox(AbstractUiWindow window, UiElement element) {
        UiHost<UiElement> elements = window.elements();
        double left = Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (int corner = 0; corner < 4; corner++) {
            AnimProps.Host local = elements.toHost(element,
                    (corner & 1) == 0 ? 0 : element.getWidth(),
                    (corner & 2) == 0 ? 0 : element.getHeight());
            left = Math.min(left, local.x());
            top = Math.min(top, local.y());
            right = Math.max(right, local.x());
            bottom = Math.max(bottom, local.y());
        }
        int x = (int) Math.floor(left);
        int y = (int) Math.floor(top);
        return screenBox(window, x, y, (int) Math.ceil(right) - x, (int) Math.ceil(bottom) - y);
    }

    /** A box of the window's own space in screen coordinates, the window's transform folded in. */
    private Rect2i screenBox(AbstractUiWindow window, int x, int y, int width, int height) {
        double left = Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (int corner = 0; corner < 4; corner++) {
            AnimProps.Host screen = toHost(window,
                    (corner & 1) == 0 ? x : x + width,
                    (corner & 2) == 0 ? y : y + height);
            left = Math.min(left, screen.x());
            top = Math.min(top, screen.y());
            right = Math.max(right, screen.x());
            bottom = Math.max(bottom, screen.y());
        }
        int sx = (int) Math.floor(left);
        int sy = (int) Math.floor(top);
        return new Rect2i(sx, sy, (int) Math.ceil(right) - sx, (int) Math.ceil(bottom) - sy);
    }

    /** The window's own box in screen coordinates. Laid out rather than measured, exactly like {@link #bounds()}. */
    private static Rect2i windowBox(IUiWindow window) {
        return new Rect2i(window.getX(), window.getY(), window.getWidth(), window.getHeight());
    }

    private static boolean overlaps(Rect2i a, Rect2i b) {
        return a.getX() < b.getX() + b.getWidth() && b.getX() < a.getX() + a.getWidth()
                && a.getY() < b.getY() + b.getHeight() && b.getY() < a.getY() + a.getHeight();
    }

    @Override
    public void bringToFront(IUiWindow window) {
        List<IUiWindow> group = withDescendants(window);
        nodes.removeAll(group);
        nodes.addAll(group);
    }

    /** The screen stack is ordered by the clicks: the window the player just used goes to the front. */
    @Override
    protected void onNodePicked(IUiWindow window) {
        bringToFront(window);
    }

    @Override
    protected void onNodeAdded(IUiWindow window) {
        if (window instanceof AbstractUiWindow ui) ui.attachHost(this);
        normalizeOrder();
        clampInto(window);
    }

    @Override
    protected void onNodeRemoved(IUiWindow window) {
        if (window instanceof AbstractUiWindow ui) ui.detachHost(this);
        if (window.getParent() instanceof AbstractUiWindow parent) parent.onChildForgotten(window);
    }

    /**
     * Keeps the rule the stacking relies on: a parent always draws under its own descendants, whichever order the
     * windows happened to be opened in. Roots keep their relative order and each subtree is pulled up behind its root.
     */
    private void normalizeOrder() {
        List<IUiWindow> ordered = new ArrayList<>(nodes.size());
        for (IUiWindow root : nodes) {
            IUiWindow parent = root.getParent();
            if (parent != null && nodes.contains(parent)) continue;
            for (IUiWindow member : withDescendants(root)) {
                if (!ordered.contains(member)) ordered.add(member);
            }
        }
        if (ordered.size() != nodes.size() || ordered.equals(nodes)) return;
        nodes.clear();
        nodes.addAll(ordered);
    }

    @Override
    protected void onHostResized() {
        for (IUiWindow window : nodes) {
            window.onRelayout(hostWidth(), hostHeight());
            clampInto(window);
        }
    }

    @Override
    protected boolean applyClip(GuiGraphics graphics, IUiWindow window, @Nullable AnimProps props) {
        if (!window.isClipped()) return false;
        if (props == null) {
            graphics.enableScissor(window.getX(), window.getY(), window.getRight(), window.getBottom());
        } else {
            AnimProps.Rect rect = props.clip(window.getX(), window.getY(), window.getWidth(), window.getHeight());
            graphics.enableScissor(rect.left(), rect.top(), rect.right(), rect.bottom());
        }
        return true;
    }

    @Override
    protected boolean handlesOutsideClick(IUiWindow window) {
        return window.handlesOutsideClicks();
    }

    @Override
    protected boolean consumeKey(IUiWindow window, int keyCode, int scanCode, int modifiers) {
        if (!mc.options.keyInventory.matches(keyCode, scanCode)) return false;
        close(window);
        return true;
    }

    @Override
    @Nullable
    protected IUiWindow focusFallback(IUiWindow closing) {
        IUiWindow parent = closing.getParent();
        return parent != null && nodes.contains(parent) ? parent : getTop();
    }

    @Override
    protected List<IUiWindow> closeTargets(IUiWindow window) {
        return withDescendants(window);
    }

    private List<IUiWindow> withDescendants(IUiWindow root) {
        List<IUiWindow> result = new ArrayList<>();
        result.add(root);
        for (int i = 0; i < result.size(); i++) {
            IUiWindow parent = result.get(i);
            for (IUiWindow candidate : nodes) {
                if (candidate.getParent() == parent && !result.contains(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private void clampInto(IUiWindow window) {
        // a window on its way out keeps the spot it was closed at
        if (!sized() || isClosing(window)) return;
        int x = Math.clamp(window.getX(), 0, Math.max(0, hostWidth() - window.getWidth()));
        int y = Math.clamp(window.getY(), 0, Math.max(0, hostHeight() - window.getHeight()));
        if (x != window.getX() || y != window.getY()) window.setPosition(x, y);
    }

    public final class GhostTarget {
        private final AbstractUiWindow window;
        private final UiElement element;
        private final GhostSlot slot;

        private GhostTarget(AbstractUiWindow window, UiElement element, GhostSlot slot) {
            this.window = window;
            this.element = element;
            this.slot = slot;
        }

        public GhostSlot slot() {
            return slot;
        }

        /**
         * The element's box in screen coordinates, recomputed rather than stored because the element may move or
         * animate between two calls.
         */
        public Rect2i area() {
            if (!element.isClipped()) return new Rect2i(0, 0, 0, 0);
            return screenBox(window, element);
        }
    }

    /** A slot of a window that the cursor is over, with the box it occupies on the screen. */
    public final class SlotTarget {
        private final ContainerWindow<?> window;
        private final Slot slot;

        private SlotTarget(ContainerWindow<?> window, Slot slot) {
            this.window = window;
            this.slot = slot;
        }

        public ItemStack stack() {
            return slot.getItem();
        }

        /** Recomputed rather than stored, because the window may move or animate between two calls. */
        public Rect2i area() {
            return screenBox(window, slot.x, slot.y, ContainerWindow.SLOT_SIZE, ContainerWindow.SLOT_SIZE);
        }
    }
}
