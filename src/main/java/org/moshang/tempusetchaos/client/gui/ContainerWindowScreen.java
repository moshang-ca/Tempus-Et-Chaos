package org.moshang.tempusetchaos.client.gui;

import lombok.Getter;
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
 * The lifecycle and input methods below intentionally mirror {@link AbstractWindowScreen}: Java has no multiple
 * inheritance, so a change to one of them has to be made in both.
 */
@ParametersAreNonnullByDefault
public abstract class ContainerWindowScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements WindowScreen {
    @Getter
    protected final WindowManager windows = new WindowManager(this);

    public ContainerWindowScreen(T menu, Inventory playerInventory, Component title) {
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
     * The slot the cursor is over and the box it occupies on the screen, or null. Both origins are zero, so the screen
     * box of a slot is its gui box as well.
     */
    @Nullable
    public WindowManager.SlotTarget slotAt(double mouseX, double mouseY) {
        return windows.slotAt(mouseX, mouseY);
    }

    /** Opens the windows of this screen: it runs on the first layout only, the windows then live until it closes. */
    protected abstract void populate();

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
     * The inherited pass draws the dimming and announces the screen to the other mods; its slot loop finds nothing
     * because {@link #isHovering(int, int, int, int, double, double)} answers {@code false}.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        windows.updateAnimations();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        // the vanilla pass is not reused, so its event is posted here
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, graphics,  mouseX, mouseY));
        windows.render(graphics, mouseX, mouseY, partialTick);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY));
    }

    /**
     * Every slot hit test of the superclass funnels through here: answering {@code false} keeps the screen out of the
     * menu, which would otherwise be driven a second time at {@code leftPos/topPos}.
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
