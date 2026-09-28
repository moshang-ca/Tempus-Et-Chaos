package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IUIWindow {
    void init(Minecraft mc, int screenWidth, int screenHeight);
    void onResize(int screenWidth, int screenHeight);
    default void tick() {}
    void onClose();
    default void onRemoved() {}

    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setPosition(int x, int y);

    default int getRight() { return getX() + getWidth(); }
    default int getBottom() { return getY() + getHeight(); }

    default boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getRight() && mouseY >= getY() && mouseY < getBottom();
    }

    default boolean isVisible() { return true; }

    @Nullable
    default IUIWindow getParent() { return null; }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    default void mouseMoved(double mouseX, double mouseY) {}
    default boolean isMouseClicked(double mouseX, double mouseY, int button) { return false; }
    default boolean isMouseReleased(double mouseX, double mouseY, int button) { return false; }
    default boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }
    default boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char codePoint, int modifiers) { return false; }
}
