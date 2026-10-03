package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;
import org.moshang.tempusetchaos.client.gui.element.GhostSlot;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class WindowManager extends UiHost<IUiWindow> {
    @Getter
    private final Screen host;

    public WindowManager(Screen host) {
        this.host = host;
    }

    public void open(IUiWindow window) {
        add(window);
    }

    public boolean hasWindow() {
        return hasNodes();
    }

    public List<IUiWindow> getWindows() {
        return getNodes();
    }

    /**
     * The box the open windows occupy by layout, in screen coordinates, their animations left out. This is what a
     * screen reports to other mods: a window that is still sliding or growing owns its place already, so the answer
     * stays steady while it moves. Null when nothing is open.
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
                if (element instanceof GhostSlot slot && slot.accepts(stack)) {
                    targets.add(new GhostTarget(ui, element, slot));
                }
            }
        }
        return targets;
    }

    /** An element of this screen that takes ghost ingredients, its area mapped out on demand. */
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
         * The element's box in screen coordinates. Recomputed rather than stored because a caller may hold on
         * to the target while the window moves, and because the element's own animations scale or turn it.
         */
        public Rect2i area() {
            UiHost<UiElement> elements = window.elements();
            double left = Double.MAX_VALUE;
            double top = Double.MAX_VALUE;
            double right = -Double.MAX_VALUE;
            double bottom = -Double.MAX_VALUE;
            for (int corner = 0; corner < 4; corner++) {
                AnimProps.Local local = elements.toHost(element,
                        (corner & 1) == 0 ? 0 : element.getWidth(),
                        (corner & 2) == 0 ? 0 : element.getHeight());
                AnimProps.Local screen = toHost(window, local.x(), local.y());
                left = Math.min(left, screen.x());
                top = Math.min(top, screen.y());
                right = Math.max(right, screen.x());
                bottom = Math.max(bottom, screen.y());
            }
            int x = (int) Math.floor(left);
            int y = (int) Math.floor(top);
            return new Rect2i(x, y, (int) Math.ceil(right) - x, (int) Math.ceil(bottom) - y);
        }
    }

    @Override
    public void bringToFront(IUiWindow window) {
        List<IUiWindow> group = withDescendants(window);
        nodes.removeAll(group);
        nodes.addAll(group);
    }

    @Override
    protected void onNodeAdded(IUiWindow window) {
        normalizeOrder();
        clampInto(window);
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
}
