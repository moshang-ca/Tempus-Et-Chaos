package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public abstract class AbstractWindowScreen extends Screen implements WindowScreen {
    protected final WindowManager windows = new WindowManager(this);

    protected AbstractWindowScreen(Component title) {
        super(title);
    }

    @Override
    public List<WindowManager.GhostTarget> ghostSlots(ItemStack stack) {
        return windows.ghostSlots(stack);
    }

    /** The box the open windows occupy by layout, in screen coordinates, their animations left out. Null when nothing is open. */
    @Override
    @Nullable
    public Rect2i windowArea() {
        return windows.bounds();
    }

    /**
     * Opens the windows of this screen. It runs on every {@link #init()}, which the client calls after a construction
     * and again whenever this screen comes back to the front or is resized, so it has to be written to build the
     * windows from scratch rather than to add to whatever is already there. The list is emptied first, the same way
     * vanilla drops its widgets before rebuilding them.
     */
    protected abstract void populate();

    @Override
    protected void init() {
        super.init();
        windows.rebuild(width, height, this::populate);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        windows.onResize(width, height);
    }

    @Override
    public void tick() {
        super.tick();
        windows.tick();
    }

    /** Keep the integrated server running, the windows read live values from it. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        windows.updateAnimations();
        super.render(graphics, mouseX, mouseY, partialTick);
        windows.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        windows.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return windows.isMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return windows.isMouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return windows.isMouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return windows.isMouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return windows.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return windows.keyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return windows.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }
}
