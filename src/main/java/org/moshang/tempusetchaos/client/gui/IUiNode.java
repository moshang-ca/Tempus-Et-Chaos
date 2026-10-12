package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.anim.AnimProps;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Anything a host can hold: windows for the screen, elements for a window.
 * Positions and sizes are host space, everything handed to the callbacks below is local to the node,
 * its own top left being (0, 0).
 */
@ParametersAreNonnullByDefault
public interface IUiNode {
    default void init(Minecraft mc, int hostWidth, int hostHeight) {}
    default void onResize(int hostWidth, int hostHeight) {}
    default void tick() {}
    default void onAdded() {}
    /** Told which host now holds this node. */
    default void onAttached(UiHost<?> host) {}
    default void onRemoved() {}
    default void onClose() {}

    /**
     * The host let go of this node to lay itself out again: not a close, whatever the node holds stays as it is.
     */
    default void onDetached() {}
    default void onFocusChanged(boolean focused) {}
    default void onHoverChanged(boolean hovered) {}

    /** Whether a click on this node moves the focus to it. A node that is not focusable lets the focus go instead. */
    default boolean takesFocus() { return true; }

    /** Asked after this node consumed a key: the node is done with the keyboard and the focus moves on. */
    default boolean releasesFocus() { return false; }

    /** The host was asked to close this node, but has not removed it yet: the exit animation starts here. */
    default void onClosing() {}

    /** Whether an exit animation is still playing, meaning the host has to keep the node around. */
    default boolean isClosing() { return false; }

    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setPosition(int x, int y);

    default int getRight() { return getX() + getWidth(); }
    default int getBottom() { return getY() + getHeight(); }

    default boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX < getWidth() && mouseY >= 0 && mouseY < getHeight();
    }

    default boolean isVisible() { return true; }

    /** Whether this node can take the pointer at all, false making it pure decoration. */
    default boolean takesPointer() { return true; }

    default boolean isClipped() { return true; }

    /**
     * The box of this node's own space its drawing is clipped to, null drawing it unclipped.
     */
    @Nullable
    default Rect2i viewport() { return null; }

    /**
     * Whether the host may drag this node with the mouse. A plain switch: the drag belongs to the host, a node only
     * says yes or no.
     */
    default boolean canDrag() { return false; }

    @Nullable
    default AnimProps getRenderProps() { return null; }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * Drawn by the host after every node has had its {@link #render}, with the node's own pose and clip left off and
     * the mouse in the host's space rather than local coordinates, so the content is placed by what it draws itself.
     */
    default void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    /**
     * Drawn by the host once every node has had its {@link #renderForeground}, for the node the mouse is on, also with
     * the node's own pose and clip left off. It is the place for content that follows the cursor past the node's own
     * bounds and still stays under whatever covers the node.
     */
    default void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    default void mouseMoved(double mouseX, double mouseY) {}
    default boolean isMouseClicked(double mouseX, double mouseY, int button) { return false; }
    default boolean isMouseReleased(double mouseX, double mouseY, int button) { return false; }
    default boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }
    default boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char codePoint, int modifiers) { return false; }

    default void updateAnimation() { }
}
