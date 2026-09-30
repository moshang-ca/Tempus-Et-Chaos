package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds a flat list of nodes living in one space: the screen for {@link WindowManager}, a window for its elements.
 * Every node is drawn at the origin of their own space, so the host translates the pose by the node position and
 * hands the node its own local coordinates.
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class UiHost<T extends IUiNode> {
    protected final Minecraft mc;
    protected final List<T> nodes = new ArrayList<>();
    private final List<T> pendingAdd = new ArrayList<>();
    private final List<T> pendingRemove = new ArrayList<>();
    private final List<T> pendingClose = new ArrayList<>();
    private final List<T> needsInit = new ArrayList<>();

    private int hostWidth;
    private int hostHeight;
    private boolean sized;
    private int dispatchDepth;

    @Nullable
    private T dragging;
    @Nullable
    private T focused;
    @Nullable
    private T hovered;

    public UiHost() {
        this.mc = Minecraft.getInstance();
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

    protected boolean sized() {
        return sized;
    }

    protected int hostWidth() {
        return hostWidth;
    }

    protected int hostHeight() {
        return hostHeight;
    }

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
            for (T node : nodes) {
                node.updateAnimation();
            }
        } finally {
            exit();
        }
    }

    public void add(T node) {
        if (dispatchDepth > 0) {
            pendingAdd.add(node);
            return;
        }
        doAdd(node);
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

    public boolean hasNodes() {
        return !nodes.isEmpty();
    }

    public void closeTop() {
        T top = getTop();
        if (top != null) close(top);
    }

    public void closeAll() {
        List<T> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onClose();
        }
    }

    public void clear() {
        List<T> doomed = detachAll();
        for (int i = doomed.size() - 1; i >= 0; i--) {
            doomed.get(i).onRemoved();
        }
    }

    public void disposeAll() {
        clear();
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

    @Nullable
    public T getHovered() {
        return hovered;
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
                if (!isInteractive(node)) continue;

                AnimProps props = node.getRenderProps();
                // render always gets the real local mouse position: the carried item and the tooltip follow the cursor even
                // outside the node, what ends up visible is decided by the stacking order below
                AnimProps.Local local = localPoint(node, mouseX, mouseY);

                // earlier nodes may write a higher gui z (items sit at +100), clear the depth they left behind
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
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
                    graphics.flush();
                } finally {
                    if (alpha < 1f) graphics.setColor(1f, 1f, 1f, 1f);
                    pose.popPose();
                    if (clipped) graphics.disableScissor();
                }
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
                // clicks outside every node still belong to the focused one, that is how vanilla reports "clicked outside the gui"
                node = getFocused();
                if (node == null || !handlesOutsideClick(node)) return false;
                AnimProps.Local local = localPoint(node, mouseX, mouseY);
                return node.isMouseClicked(local.x(), local.y(), button);
            }
            setFocused(node);
            AnimProps.Local local = localPoint(node, mouseX, mouseY);
            if (!node.isMouseClicked(local.x(), local.y(), button)) return false;
            onNodePicked(node);
            if (button == 0) dragging = node;
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
            return captured.isMouseDragged(local.x(), local.y(), button, dragX, dragY);
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
            if (captured == null) return false;
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

    protected boolean isInteractive(T node) {
        if (!node.isVisible()) return false;
        AnimProps props = node.getRenderProps();
        return props == null || props.isVisible();
    }

    /** Host space point -> the node's own space. */
    protected AnimProps.Local localPoint(T node, double mouseX, double mouseY) {
        AnimProps props = node.getRenderProps();
        if (props == null || props.isIdentity()) {
            return new AnimProps.Local(mouseX - node.getX(), mouseY - node.getY());
        }
        return props.toLocal(mouseX, mouseY, node.getX(), node.getY(), node.getWidth(), node.getHeight());
    }

    /** Scissor the node, in host space. Returns whether a scissor was enabled. */
    protected boolean applyClip(GuiGraphics graphics, T node, @Nullable AnimProps props) {
        return false;
    }

    protected void onNodeAdded(T node) {}

    protected void onNodeRemoved(T node) {}

    protected void onNodePicked(T node) {
        bringToFront(node);
    }

    protected boolean handlesOutsideClick(T node) {
        return false;
    }

    protected boolean consumeKey(T node, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Who takes the focus after {@code closing} is gone. */
    @Nullable
    protected T focusFallback(T closing) {
        return getTop();
    }

    /** Which nodes a close should take down with it. */
    protected List<T> closeTargets(T node) {
        return List.of(node);
    }

    private void doAdd(T node) {
        nodes.add(node);
        node.onAdded();
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
            T closing = doomed.get(i);
            needsInit.remove(closing);
            pendingAdd.remove(closing);
            if (nodes.remove(closing)) {
                closing.onClose();
                onNodeRemoved(closing);
            }
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
        while (!pendingAdd.isEmpty() || !pendingRemove.isEmpty() || !pendingClose.isEmpty()) {
            List<T> adds = new ArrayList<>(pendingAdd);
            List<T> removes = new ArrayList<>(pendingRemove);
            List<T> closes = new ArrayList<>(pendingClose);
            pendingAdd.clear();
            pendingRemove.clear();
            pendingClose.clear();
            for (T node : adds) doAdd(node);
            for (T node : removes) doRemove(node);
            for (T node : closes) doClose(node);
        }
    }
}
