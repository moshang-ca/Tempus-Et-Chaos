package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class AbstractContainerWindow<T extends AbstractContainerMenu> implements IUIWindow {
    protected final T menu;
    private final Host host;

    @Nullable
    private Minecraft mc;
    private boolean dragging;
    private double grabX;
    private double grabY;

    protected AbstractContainerWindow(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        this.menu = menu;
        this.host = new Host(menu, playerInventory, title, imageWidth, imageHeight);
    }

    @Override
    public void init(Minecraft mc, int screenWidth, int screenHeight) {
        this.mc = mc;
        host.init(mc, screenWidth, screenHeight);
    }

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        if (mc != null) host.resize(mc, screenWidth, screenHeight);
    }

    @Override
    public void tick() {
        host.tick();
    }

    @Override
    public void onClose() {
        dispose();
    }

    @Override
    public void onRemoved() {
        dispose();
    }

    @Override
    public int getX() {
        return host.windowX();
    }

    @Override
    public int getY() {
        return host.windowY();
    }

    @Override
    public int getWidth() {
        return host.getXSize();
    }

    @Override
    public int getHeight() {
        return host.getYSize();
    }

    @Override
    public void setPosition(int x, int y) {
        host.moveTo(x, y);
    }

    @Override
    public boolean isClipped() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        host.renderWithTooltip(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean isMouseClicked(double mouseX, double mouseY, int button) {
        // any spot that is not a slot is a drag handle, so the image stays exactly where vanilla puts it
        if (button == 0 && isMouseOver(mouseX, mouseY) && !host.overSlot(mouseX, mouseY)) {
            dragging = true;
            grabX = mouseX - getX();
            grabY = mouseY - getY();
            return true;
        }
        return host.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return host.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            setPosition((int) (mouseX - grabX), (int) (mouseY - grabY));
            return true;
        }
        return host.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return host.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // let the close key fall through to the window manager, the delegate would close the whole screen
        if (mc != null && mc.options.keyInventory.matches(keyCode, scanCode)) return false;
        return host.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return host.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return host.charTyped(codePoint, modifiers);
    }

    protected int leftPos() {
        return host.windowX();
    }

    protected int topPos() {
        return host.windowY();
    }

    protected int imageWidth() {
        return host.getXSize();
    }

    protected int imageHeight() {
        return host.getYSize();
    }

    protected abstract void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        host.vanillaLabels(graphics, mouseX, mouseY);
    }

    private void dispose() {
        if (mc != null && mc.player != null && mc.player.containerMenu == menu && mc.getConnection() != null) {
            mc.player.closeContainer();
        }
        host.removed();
    }

    private final class Host extends AbstractContainerScreen<T> {
        private int posX;
        private int posY;
        private boolean placed;

        Host(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
            super(menu, playerInventory, title);
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        @Override
        protected void init() {
            super.init();
            applyPosition();
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBg(graphics, partialTick, mouseX, mouseY);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            AbstractContainerWindow.this.renderBg(graphics, partialTick, mouseX, mouseY);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            AbstractContainerWindow.this.renderLabels(graphics, mouseX, mouseY);
        }

        void vanillaLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderLabels(graphics, mouseX, mouseY);
        }

        boolean overSlot(double mouseX, double mouseY) {
            for (Slot slot : menu.slots) {
                if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) return true;
            }
            return false;
        }

        void moveTo(int x, int y) {
            posX = x;
            posY = y;
            placed = true;
            applyPosition();
        }

        int windowX() {
            return placed ? posX : leftPos;
        }

        int windowY() {
            return placed ? posY : topPos;
        }

        private void applyPosition() {
            if (!placed) return;
            leftPos = posX;
            topPos = posY;
        }
    }
}
