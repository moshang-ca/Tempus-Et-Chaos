package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.gui.anim.*;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;

// Temporary manual-verification harness. Delete this file to remove the demo entirely.
@ParametersAreNonnullByDefault
public class DemoWindowScreen extends AbstractWindowScreen {
    private static final int TITLE_HEIGHT = 12;

    public DemoWindowScreen() {
        super(Component.literal("Window Demo"));
        windows.open(new DemoPanel(windows, "parent", null, 40, 40, 170, 96, true));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            IUiWindow inventory = new InventoryWindow(mc.player.inventoryMenu, mc.player.getInventory(),
                    Component.literal("Inventory"), 176, 166);
            inventory.setPosition(300, 40);
            windows.open(inventory);
        }
    }

    @EventBusSubscriber(modid = TempusEtChaos.MODID, value = Dist.CLIENT)
    public static class Opener {
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS || event.getKey() != GLFW.GLFW_KEY_F10) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return;
            mc.setScreen(new DemoWindowScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static class DemoPanel extends AbstractUiWindow {
        private final WindowManager manager;
        private final String title;
        @Nullable
        private final IUiWindow parent;
        private final int width;
        private final int height;
        private final boolean spawnsChild;
        private final StringBuilder typed = new StringBuilder();

        private final AnimProps props = new AnimProps();
        private final AnimValue fade = new AnimValue(0f);
        private final Track wobble = new Track();

        private int x;
        private int y;
        private boolean dragging;
        private double grabX;
        private double grabY;

        DemoPanel(WindowManager manager, String title, @Nullable IUiWindow parent, int x, int y, int width, int height, boolean spawnsChild) {
            this.manager = manager;
            this.title = title;
            this.parent = parent;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.spawnsChild = spawnsChild;

            wobble.key(0.0f, 0f, Easing.EASE_IN_OUT_QUAD)
                    .key(0.3f, 0.2f, Easing.EASE_OUT_QUAD)
                    .key(0.6f, 0.5f, Easing.EASE_IN_OUT_QUAD)
                    .key(1.0f, 1f, Easing.EASE_OUT_QUAD);

            elements.add(new DemoLabel("label element", 4, 34));
            elements.add(new DemoButton(4, 48));
        }

        @Override
        public void init(Minecraft mc, int screenWidth, int screenHeight) {
            if (parent != null) setPosition(parent.getX() + 20, parent.getY() + 20);
            wobble.play(500, false, false);
            fade.to(1f, 250, Easing.EASE_OUT_QUAD);
            super.init(mc, screenWidth, screenHeight);
        }

        @Nullable
        @Override
        public IUiWindow getParent() { return parent; }

        @Override
        public int getX() { return x; }

        @Override
        public int getY() { return y; }

        @Override
        public int getWidth() { return width; }

        @Override
        public int getHeight() { return height; }

        @Override
        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void updateAnimation() {
            super.updateAnimation();
            fade.update();
            wobble.update();

//            props.alpha = fade.getValue();
//            props.scaleX = wobble.getCurrent();
//            props.scaleY = wobble.getCurrent();
        }

        @Override
        public AnimProps getRenderProps() {
            return props;
        }

        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF202020);
            graphics.fill(0, 0, width, TITLE_HEIGHT, manager.isFocused(this) ? 0xFF3A3A8C : 0xFF2A2A50);
            graphics.drawString(Minecraft.getInstance().font, title, 4, 2, 0xFFFFFF);
            graphics.drawString(Minecraft.getInstance().font, "typed: " + typed, 4, TITLE_HEIGHT + 4, 0xE0E0E0);
            if (spawnsChild) {
                graphics.fill(childButtonLeft(), childButtonTop(), childButtonLeft() + 60, childButtonTop() + 12, 0xFF505050);
                graphics.drawString(Minecraft.getInstance().font, "open child", childButtonLeft() + 4, childButtonTop() + 2, 0xFFFFFF);
            }
        }

        @Override
        protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            if (mouseY < TITLE_HEIGHT) {
                dragging = true;
                grabX = mouseX;
                grabY = mouseY;
                return true;
            }
            if (spawnsChild && mouseX >= childButtonLeft() && mouseX < childButtonLeft() + 60
                    && mouseY >= childButtonTop() && mouseY < childButtonTop() + 12) {
                manager.open(new DemoPanel(manager, "child", this, 0, 0, 150, 76, false));
                return true;
            }
            return false;
        }

        @Override
        public boolean isMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (!dragging) return super.isMouseDragged(mouseX, mouseY, button, dragX, dragY);
            setPosition((int) Math.round(x + (mouseX - grabX)), (int) Math.round(y + (mouseY - grabY)));
            return true;
        }

        @Override
        public boolean isMouseReleased(double mouseX, double mouseY, int button) {
            if (!dragging) return super.isMouseReleased(mouseX, mouseY, button);
            dragging = false;
            return true;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            typed.append(codePoint);
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typed.isEmpty()) {
                typed.deleteCharAt(typed.length() - 1);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private int childButtonLeft() { return 4; }

        private int childButtonTop() { return TITLE_HEIGHT + 20; }
    }

    static class DemoLabel extends UiElement {
        private final String text;
        private final AnimValue fade = new AnimValue(0f);

        DemoLabel(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = 76;
            this.height = 10;
        }

        @Override
        public void onAdded() {
            fade.to(1f, 400, Easing.EASE_OUT_QUAD);
        }

        @Override
        public void updateAnimation() {
            fade.update();
            renderProps.alpha = fade.getValue();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, getWidth(), getHeight(), 0x40FFFFFF);
            graphics.drawString(Minecraft.getInstance().font, text, 2, 1, 0xFFFFFF);
        }
    }

    /** Hover scales it, clicking makes it spin while being stretched: the hit box must keep matching the drawing. */
    static class DemoButton extends UiElement {
        private final AnimValue scale = new AnimValue(1f);
        private final AnimValue spin = new AnimValue(0f);
        private boolean stretched;
        private int clicks;

        DemoButton(int x, int y) {
            this.x = x;
            this.y = y;
            this.width = 60;
            this.height = 14;
        }

        @Override
        public void onHoverChanged(boolean hovered) {
            scale.to(hovered ? 1.15f : 1f, 150, Easing.EASE_OUT_QUAD);
        }

        @Override
        public void updateAnimation() {
            scale.update();
            spin.update();
            renderProps.scaleX = scale.getValue() * (stretched ? 2f : 1f);
            renderProps.scaleY = scale.getValue();
            renderProps.rotation = spin.getValue();
        }

        @Override
        public boolean isMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            clicks++;
            stretched = !stretched;
            spin.to(stretched ? (float) (Math.PI / 2) : 0f, 250, Easing.EASE_IN_OUT_QUAD);
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, getWidth(), getHeight(), stretched ? 0xFF7A3A3A : 0xFF3A5A3A);
            graphics.drawString(Minecraft.getInstance().font, "clicks " + clicks, 2, 3, 0xFFFFFF);
        }
    }

    static class InventoryWindow extends AbstractContainerWindow<InventoryMenu> {
        private final AnimProps props = new AnimProps();
        private final AnimValue scaled = new AnimValue(0.1f);

        InventoryWindow(InventoryMenu menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
            super(menu, playerInventory, title, imageWidth, imageHeight);
        }

        @Override
        public void init(Minecraft mc, int screenWidth, int screenHeight) {
            super.init(mc, screenWidth, screenHeight);
            scaled.to(1f, 500, Easing.EASE_IN_OUT_QUAD);
        }

        @Override
        public void updateAnimation() {
            super.updateAnimation();
            scaled.update();
            props.scaleX = scaled.getValue();
            props.scaleY = scaled.getValue();
        }

        @Override
        public @Nullable AnimProps getRenderProps() {
            return props;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, leftPos(), topPos(), 0, 0, imageWidth(), imageHeight());
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {

        }
    }
}
