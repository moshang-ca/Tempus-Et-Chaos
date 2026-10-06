package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * The screen a container window lives on: it owns the menu and registers it with the game, while the windows draw
 * and drive it. Both origins stay at zero, because the slots belong to a window's own space, not to the screen's.
 * <p>
 * A subclass implements {@code MenuProvider} and returns one of these from {@code createMenu}, and opens its windows
 * from its own constructor.
 * <p>
 * The lifecycle and input methods below intentionally mirror {@link AbstractWindowScreen}: Java has no multiple
 * inheritance, so a change to one of them has to be made in both.
 */
@ParametersAreNonnullByDefault
public abstract class ContainerWindowScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements WindowScreen {
    protected final WindowManager windows = new WindowManager(this);

    protected ContainerWindowScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** The ghost slots of this screen that take the stack, in screen coordinates. */
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
     * and again whenever this screen comes back to the front or is resized.
     */
    protected abstract void populate();

    /**
     * The windows are rebuilt here instead of being built once in the constructor.
     */
    @Override
    protected void init() {
        super.init();
        // the screen has no art of its own, every slot is drawn in the space of the window that owns it
        leftPos = 0;
        topPos = 0;
        windows.rebuild(width, height, this::populate);
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        windows.onResize(width, height);
    }

    @Override
    protected void containerTick() {
        windows.tick();
    }

    /**
     * Swapping to another screen calls the superclass version too, so the menu is only released when this screen is
     * really closed and not when another mod merely steps in front of it.
     */
    @Override
    public void onClose() {
        windows.disposeAll();
        if (minecraft != null && minecraft.player != null) menu.removed(minecraft.player);
        super.onClose();
    }

    /**
     * The inherited pass draws the dimming, runs the widget layer and announces the screen to the other mods; its
     * slot loop finds nothing because {@link #isHovering(int, int, int, int, double, double)} answers {@code false}.
     * The windows are painted over it.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        windows.updateAnimations();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        // As the vanilla render implementation will render the inventory by default,
        // we do not reuse the vanilla implementation.
        // So we should post the render event manually.
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, graphics,  mouseX, mouseY));
        windows.render(graphics, mouseX, mouseY, partialTick);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Every slot hit test of the superclass funnels through here, so answering {@code false} is what keeps the
     * screen out of the menu: it would otherwise hit test at {@code leftPos/topPos} and drive a second, invisible
     * copy of the menu at the screen origin. The windows are the only ones that touch the menu, and they do their
     * own hit testing in their own space.
     */
    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return false;
    }

    /** The screen is a bare stage, the windows paint everything, so it has no background texture. */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}

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
        if (windows.isMouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (windows.isMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (windows.isMouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (windows.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (windows.keyReleased(keyCode, scanCode, modifiers)) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (windows.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }
}
