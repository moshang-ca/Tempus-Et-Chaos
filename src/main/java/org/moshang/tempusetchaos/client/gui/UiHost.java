package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.moshang.tempusetchaos.client.gui.anim.Anim;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;
import org.moshang.tempusetchaos.client.gui.anim.Animated;
import org.moshang.tempusetchaos.config.Config;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds a flat list of nodes living in one space: the screen for {@link WindowManager}, a window for its elements.
 * Every node is drawn at the origin of their own space, so the host translates the pose by the node position and
 * hands the node its own local coordinates.
 */
@Accessors(fluent = true, chain = true)
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class UiHost<T extends IUiNode> {
    protected final Minecraft mc;
    protected final List<T> nodes = new ArrayList<>();
    private final List<T> pendingAdd = new ArrayList<>();
    private final List<T> pendingRemove = new ArrayList<>();
    private final List<T> pendingClose = new ArrayList<>();
    private final List<T> needsInit = new ArrayList<>();
    /** Nodes whose exit animation is still playing: still drawn, no longer interactive, not removed yet. */
    private final Set<T> closing = new LinkedHashSet<>();

    @Getter
    private int hostWidth;
    @Getter
    private int hostHeight;
    @Getter
    private boolean sized;
    private int dispatchDepth;

    @Setter @Getter
    protected boolean canAnimate = true;
    /** Whether the host is settling, always be true when host rebuilding. */
    protected boolean settling = false;

    @Nullable
    private T dragging;
    /** Where the drag was grabbed, in the dragged node's own space. */
    private double grabX;
    private double grabY;
    @Nullable
    private T focused;
    @Nullable
    @Accessors(fluent = false) @Getter
    private T hovered;

    public UiHost() {
        this.mc = Minecraft.getInstance();
    }

    /**
     * Drops the depth the node just wrote, so whatever is painted next can cover it. Items sit above the
     * plain gui layer and guessing their z is fragile, so the depth buffer is cleared instead.
     */
    public static void clearContentDepth(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    public void init(int hostWidth, int hostHeight) {
        updateSize(hostWidth, hostHeight);
        flushNeedsInit();
    }

    public void onResize(int hostWidth, int hostHeight) {
        updateSize(hostWidth, hostHeight);
        flushNeedsInit();
        for (T node : nodes) {
            node.onResize(hostWidth, hostHeight);
        }
        onHostResized();
    }

    protected void onHostResized() {}

    public void tick() {
        enter();
        try {
            for (T node : nodes) {
                if (node.isVisible()) node.tick();
            }
        } finally {
            exit();
        }
    }

    public void updateAnimations() {
        enter();
        try {
            boolean animate = animationsEnabled();
            for (T node : nodes) {
                if (node instanceof Animated animated) animated.anims().setEnabled(animate);
                node.updateAnimation();
            }
            finishClosings();
        } finally {
            exit();
        }
    }

    protected boolean animationsEnabled() {
        return canAnimate && !Config.CONFIG.disableAnimations.get();
    }

    /** Drop every node whose exit animation just ended. */
    private void finishClosings() {
        if (closing.isEmpty()) return;
        List<T> done = null;
        for (T node : closing) {
            if (node.isClosing()) continue;
            if (done == null) done = new ArrayList<>();
            done.add(node);
        }
        if (done == null) return;
        for (T node : done) {
            closing.remove(node);
            if (nodes.remove(node)) {
                node.onClose();
                onNodeRemoved(node);
            }
        }
        checkEmpty();
    }

    public UiHost<T> add(T node) {
        if (dispatchDepth > 0) {
            pendingAdd.add(node);
        } else {
            doAdd(node);
        }
        return this;
    }

    public void remove(T node) {
        if (dispatchDepth > 0) {
            pendingRemove.add(node);
            return;
        }
        doRemove(node);
    }

    public void close(T node) {
        if (dispatchDepth > 0) {
            pendingClose.add(node);
            return;
        }
        doClose(node);
    }

    public List<T> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /** Whether every node is gone, the ones still playing an exit animation included. */
    public boolean isEmpty() {
        return nodes.isEmpty() && closing.isEmpty();
    }

    /**
     * Takes every node out without closing it: the node is told it lost the host, nothing is torn down.
     */
    public void detachAllNodes() {
        List<T> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onDetached();
        }
    }

    /** Drops every node for good: this is the way out, and each node is told it is closing. */
    public void disposeAll() {
        List<T> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onClose();
        }
    }

    @Nullable
    public T getTop() {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            T node = nodes.get(i);
            if (isInteractive(node)) return node;
        }
        return null;
    }

    public boolean isFocused(T node) {
        return focused == node;
    }

    @Nullable
    public T getFocused() {
        return focused != null && nodes.contains(focused) && isInteractive(focused) ? focused : null;
    }

    public void setFocused(@Nullable T node) {
        if (node != null && (!nodes.contains(node) || !isInteractive(node))) return;
        if (focused == node) return;
        T previous = focused;
        focused = node;
        if (previous != null) previous.onFocusChanged(false);
        if (node != null) node.onFocusChanged(true);
    }

    public void bringToFront(T node) {
        if (!nodes.contains(node)) return;
        nodes.remove(node);
        nodes.add(node);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        enter();
        try {
            for (T node : nodes) {
                if (!isRenderable(node)) continue;

                AnimProps props = node.getRenderProps();
                // render always gets the real local mouse position: the carried item and the tooltip follow the cursor even
                // outside the node, what ends up visible is decided by the stacking order below
                AnimProps.Local local = localPoint(node, mouseX, mouseY);

                // earlier nodes may write a higher gui z (items sit above the gui layer), drop that depth again
                clearContentDepth(graphics);
                boolean clipped = applyClip(graphics, node, props);

                var pose = graphics.pose();
                pose.pushPose();
                if (props == null) {
                    pose.translate(node.getX(), node.getY(), 0f);
                } else {
                    props.apply(pose, node.getX(), node.getY(), node.getWidth(), node.getHeight());
                }
                float alpha = props == null ? 1f : props.alpha;
                if (alpha < 1f) graphics.setColor(1f, 1f, 1f, alpha);

                try {
                    node.render(graphics, (int) local.x(), (int) local.y(), partialTick);
                    if (props != null && props.overlay > Anim.EPSILON) {
                        int overlay = Math.clamp(Math.round(props.overlay * 255f), 0, 255) << 24 | 0xFFFFFF;
                        clearContentDepth(graphics);
                        graphics.fill(0, 0, node.getWidth(), node.getHeight(), overlay);
                    }
                    graphics.flush();
                } finally {
                    if (alpha < 1f) graphics.setColor(1f, 1f, 1f, 1f);
                    pose.popPose();
                    if (clipped) graphics.disableScissor();
                }
            }

            // unclipped and unposed passes: first whatever has to sit above the whole screen, then the cursor's tooltip
            for (T node : nodes) {
                if (!isRenderable(node)) continue;
                clearContentDepth(graphics);
                node.renderForeground(graphics, mouseX, mouseY, partialTick);
                graphics.flush();
            }

            T overlay = topmostAt(mouseX, mouseY);
            if (overlay != null) {
                clearContentDepth(graphics);
                overlay.renderOverlay(graphics, mouseX, mouseY, partialTick);
                graphics.flush();
            }
        } finally {
            exit();
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        enter();
        try {
            T top = topmostAt(mouseX, mouseY);
            if (top != hovered) {
                if (hovered != null) hovered.onHoverChanged(false);
                hovered = top;
                if (top != null) top.onHoverChanged(true);
            }
            for (T node : nodes) {
                if (!isInteractive(node)) continue;
                AnimProps.Local local = localPoint(node, mouseX, mouseY);
                node.mouseMoved(local.x(), local.y());
            }
        } finally {
            exit();
        }
    }

    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        enter();
        try {
            T node = topmostAt(mouseX, mouseY);
            if (node == null) {
                // nothing under the cursor: this is the click that takes the focus away. It stays on the node
                // it is already on only when that node asks for clicks outside itself.
                T focused = getFocused();
                if (focused == null || !handlesOutsideClick(focused)) {
                    setFocused(null);
                    return false;
                }
                AnimProps.Local local = localPoint(focused, mouseX, mouseY);
                return focused.isMouseClicked(local.x(), local.y(), button);
            }
            AnimProps.Local local = localPoint(node, mouseX, mouseY);
            boolean accepted = node.isMouseClicked(local.x(), local.y(), button);
            setFocused(node.takesFocus() ? node : null);
            if (!accepted) return false;
            onNodePicked(node);
            if (button == 0) {
                dragging = node;
                // the grab is remembered in the node's own space, so a moving node keeps following the cursor
                grabX = local.x();
                grabY = local.y();
            }
            return true;
        } finally {
            exit();
        }
    }

    public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        enter();
        try {
            T captured = button == 0 ? dragging : null;
            if (captured == null) captured = topmostAt(mouseX, mouseY);
            if (captured == null) return false;
            AnimProps.Local local = localPoint(captured, mouseX, mouseY);
            // the node gets the first say; a node that asked to be draggable and does not handle the drag itself
            // is moved here, which is the only place a drag is ever applied
            if (captured.isMouseDragged(local.x(), local.y(), button, dragX, dragY)) return true;
            if (captured != dragging || !captured.canDrag()) return false;
            // the grab was taken in the node's own space, so the same space gives back how far it moved
            double movedX = local.x() - grabX;
            double movedY = local.y() - grabY;
            captured.setPosition((int) Math.round(captured.getX() + movedX),
                    (int) Math.round(captured.getY() + movedY));
            return true;
        } finally {
            exit();
        }
    }

    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        enter();
        try {
            T captured = button == 0 ? dragging : null;
            if (button == 0) dragging = null;
            if (captured == null) captured = topmostAt(mouseX, mouseY);
            if (captured == null) {
                // a press outside every node belonged to the focused one, so its release does too
                T focused = getFocused();
                if (focused == null || !handlesOutsideClick(focused)) return false;
                captured = focused;
            }
            AnimProps.Local local = localPoint(captured, mouseX, mouseY);
            return captured.isMouseReleased(local.x(), local.y(), button);
        } finally {
            exit();
        }
    }

    public boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        enter();
        try {
            T node = topmostAt(mouseX, mouseY);
            if (node == null) return false;
            AnimProps.Local local = localPoint(node, mouseX, mouseY);
            return node.isMouseScrolled(local.x(), local.y(), scrollX, scrollY);
        } finally {
            exit();
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        enter();
        try {
            T node = focusOrTop();
            if (node == null) return false;
            if (node.keyPressed(keyCode, scanCode, modifiers)) return true;
            return consumeKey(node, keyCode, scanCode, modifiers);
        } finally {
            exit();
        }
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        enter();
        try {
            T node = focusOrTop();
            return node != null && node.keyReleased(keyCode, scanCode, modifiers);
        } finally {
            exit();
        }
    }

    public boolean charTyped(char codePoint, int modifiers) {
        enter();
        try {
            T node = focusOrTop();
            return node != null && node.charTyped(codePoint, modifiers);
        } finally {
            exit();
        }
    }

    @Nullable
    protected T focusOrTop() {
        T node = getFocused();
        return node != null ? node : getTop();
    }

    @Nullable
    protected T topmostAt(double mouseX, double mouseY) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            T node = nodes.get(i);
            if (hits(node, mouseX, mouseY)) return node;
        }
        return null;
    }

    protected boolean hits(T node, double mouseX, double mouseY) {
        if (!isInteractive(node)) return false;
        AnimProps.Local local = localPoint(node, mouseX, mouseY);
        return node.isMouseOver(local.x(), local.y());
    }

    /** Whether the node can be drawn at all. A closing node still can, it is only kept out of the input path. */
    protected boolean isRenderable(T node) {
        if (!node.isVisible()) return false;
        AnimProps props = node.getRenderProps();
        return props == null || props.isVisible();
    }

    protected boolean isInteractive(T node) {
        return isRenderable(node) && !closing.contains(node);
    }

    protected final boolean isClosing(T node) {
        return closing.contains(node);
    }

    protected AnimProps.Local localPoint(T node, double mouseX, double mouseY) {
        AnimProps props = node.getRenderProps();
        if (props == null || props.isIdentity()) {
            return new AnimProps.Local(mouseX - node.getX(), mouseY - node.getY());
        }
        return props.toLocal(mouseX, mouseY, node.getX(), node.getY(), node.getWidth(), node.getHeight());
    }

    public AnimProps.Local toHost(T node, double localX, double localY) {
        AnimProps props = node.getRenderProps();
        if (props == null || props.isIdentity()) return new AnimProps.Local(localX + node.getX(), localY + node.getY());
        return props.toHost(localX, localY, node.getX(), node.getY(), node.getWidth(), node.getHeight());
    }

    protected boolean applyClip(GuiGraphics graphics, T node, @Nullable AnimProps props) {
        return false;
    }

    protected void onNodeAdded(T node) {}

    protected void onNodeRemoved(T node) {}

    protected void onAllNodesGone() {}

    protected void onNodePicked(T node) {
        bringToFront(node);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean handlesOutsideClick(T node) {
        return false;
    }

    protected boolean consumeKey(T node, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Nullable
    protected T focusFallback(T closing) {
        return getTop();
    }

    protected List<T> closeTargets(T node) {
        return List.of(node);
    }

    private void doAdd(T node) {
        nodes.add(node);
        node.onAdded();
        // a rebuilt host puts its nodes straight into place: the entry animations are triggered as usual, but the
        // node skips to their end state instead of playing them again on every relayout
        if (settling && node instanceof Animated animated) animated.anims().settle();
        if (sized) {
            node.init(mc, hostWidth, hostHeight);
        } else {
            needsInit.add(node);
        }
        onNodeAdded(node);
        setFocused(node);
    }

    private void doRemove(T node) {
        needsInit.remove(node);
        pendingAdd.remove(node);
        closing.remove(node);
        if (dragging == node) dragging = null;
        if (hovered == node) hovered = null;
        boolean focusLost = focused == node;
        if (focusLost) {
            node.onFocusChanged(false);
            focused = null;
        }
        if (nodes.remove(node)) {
            node.onRemoved();
            onNodeRemoved(node);
        }
        if (focusLost) setFocused(focusFallback(node));
    }

    private void doClose(T node) {
        List<T> doomed = closeTargets(node);
        T previous = focused;
        boolean focusLost = previous != null && doomed.contains(previous);
        if (focusLost) {
            previous.onFocusChanged(false);
            focused = null;
        }
        if (dragging != null && doomed.contains(dragging)) dragging = null;
        if (hovered != null && doomed.contains(hovered)) hovered = null;
        for (int i = doomed.size() - 1; i >= 0; i--) {
            T target = doomed.get(i);
            needsInit.remove(target);
            pendingAdd.remove(target);
            if (closing.contains(target) || !nodes.contains(target)) continue;
            target.onClosing();
            if (target.isClosing()) {
                // the exit animation keeps it rendered, but it is already out of the input path and unfocused
                closing.add(target);
                continue;
            }
            nodes.remove(target);
            target.onClose();
            onNodeRemoved(target);
        }
        if (focusLost) setFocused(focusFallback(node));
    }

    private List<T> detachAll() {
        List<T> doomed = new ArrayList<>(nodes);
        nodes.clear();
        pendingAdd.clear();
        pendingRemove.clear();
        pendingClose.clear();
        needsInit.clear();
        closing.clear();
        dragging = null;
        hovered = null;
        focused = null;
        return doomed;
    }

    private void updateSize(int width, int height) {
        hostWidth = width;
        hostHeight = height;
        sized = width > 0 && height > 0;
    }

    private void flushNeedsInit() {
        if (needsInit.isEmpty()) return;
        List<T> waiting = new ArrayList<>(needsInit);
        needsInit.clear();
        if (!sized) {
            needsInit.addAll(waiting);
            return;
        }
        for (T node : waiting) {
            if (!nodes.contains(node)) continue;
            node.init(mc, hostWidth, hostHeight);
            onNodeAdded(node);
        }
    }

    private void enter() {
        dispatchDepth++;
    }

    private void exit() {
        dispatchDepth--;
        if (dispatchDepth == 0) flush();
    }

    private void flush() {
        boolean closed = false;
        while (!pendingAdd.isEmpty() || !pendingRemove.isEmpty() || !pendingClose.isEmpty()) {
            List<T> adds = new ArrayList<>(pendingAdd);
            List<T> removes = new ArrayList<>(pendingRemove);
            List<T> closes = new ArrayList<>(pendingClose);
            pendingAdd.clear();
            pendingRemove.clear();
            pendingClose.clear();
            for (T node : adds) doAdd(node);
            for (T node : removes) doRemove(node);
            for (T node : closes) {
                doClose(node);
                closed = true;
            }
        }
        if (closed) checkEmpty();
    }

    private void checkEmpty() {
        if (nodes.isEmpty() && closing.isEmpty()) onAllNodesGone();
    }
}
