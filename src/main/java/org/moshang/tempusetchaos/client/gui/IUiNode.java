package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
    default void onRemoved() {}
    default void onClose() {}
    default void onFocusChanged(boolean focused) {}
    default void onHoverChanged(boolean hovered) {}

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

    default boolean isClipped() { return true; }

    @Nullable
    default AnimProps getRenderProps() { return null; }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * Drawn by the host after every node has had its {@link #render}, with no clip and no pose, and with the mouse in
     * host space. This is the place for content that belongs above the whole screen instead of inside the node's own
     * stacking, like the item on the cursor.
     */
    default void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    /**
     * Drawn by the host once every node has had its {@link #renderForeground}, for the node the mouse is on, with no
     * clip and no pose, and with the mouse in host space. This is the place for tooltips: they follow the cursor past
     * the node's own bounds, but stay under whatever covers the node.
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
