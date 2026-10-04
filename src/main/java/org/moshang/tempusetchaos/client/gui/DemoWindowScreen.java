package org.moshang.tempusetchaos.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.client.gui.anim.*;
import org.moshang.tempusetchaos.client.gui.element.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * <h1><b>This should not be published</b></h1>
 * Temporary manual-verification harness. If new ui feature is added, we can test here.
 */
@ParametersAreNonnullByDefault
public class DemoWindowScreen extends AbstractWindowScreen {
    private static final int TITLE_HEIGHT = 12;

    /** The intro: the icon turns on its own first, then the panel opens out of it while its blocks slide to the corners. */
    private static final Cue SPIN = new Cue(0, 400, Easing.EASE_OUT_CUBIC);
    private static final Cue OPEN = Cue.after(SPIN, 64, 336, Easing.EASE_OUT_EXPO);
    private static final float TURNS = (float) (Math.PI * 4);
    private static final int ICON_SIZE = 10;
    private static final int ICON_BLOCK = 6;

    public DemoWindowScreen() {
        super(Component.literal("Window Demo"));
    }

    @Override
    protected void populate() {
        windows.open(new DemoPanel(windows, "parent", null, 0, 0, 170, 180, true));
    }

    @EventBusSubscriber(modid = TempusEtChaos.MODID, value = Dist.CLIENT)
    public static class Opener {
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;
            if (event.getKey() == GLFW.GLFW_KEY_F10) mc.setScreen(new DemoWindowScreen());
            else if (event.getKey() == GLFW.GLFW_KEY_F9) mc.setScreen(new DemoContainerScreen(mc.player.inventoryMenu, mc.player.getInventory(), Component.literal("inventory")));
        }
    }

    /** The other half of the contract: this screen owns the menu and hands it to a window. */
    public static class DemoContainerScreen extends ContainerWindowScreen<InventoryMenu> {
        public DemoContainerScreen(InventoryMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void populate() {
            windows.open(new DemoContainerWindow(menu, 176, 166));
        }
    }

    static class DemoContainerWindow extends ContainerWindow<InventoryMenu> {
        DemoContainerWindow(InventoryMenu menu, int width, int height) {
            super(menu, width, height);
            anims().add(Anims.popIn(500, Easing.EASE_IN_OUT_QUAD));
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, 0, 0, 0, 0, getWidth(), getHeight());
        }
    }

    /** Four animations at once: fade in, a keyframed settle, and an exit pair the host waits for. */
    static class DemoPanel extends AbstractUiWindow {
        private static final ItemStack DEMO_STACK = new ItemStack(Items.DIAMOND);

        private final WindowManager manager;
        private final String title;
        @Nullable
        private final IUiWindow parent;
        private final int width;
        private final int height;
        private final boolean spawnsChild;

        private int x;
        private int y;
        private boolean dragging;
        private double grabX;
        private double grabY;

        private int inputFieldRes = 0;
        private float gaugeRatio = 0;
        private boolean reversed = false;
        private ItemStack ghost = ItemStack.EMPTY;

        DemoPanel(WindowManager manager, String title, @Nullable IUiWindow parent, int x, int y, int width, int height, boolean spawnsChild) {
            this.manager = manager;
            this.title = title;
            this.parent = parent;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.spawnsChild = spawnsChild;

            Track wobble = new Track();
            wobble.key(0.0f, -6f, Easing.EASE_OUT_QUAD)
                    .key(0.5f, 2f, Easing.EASE_IN_OUT_QUAD)
                    .key(1.0f, 0f, Easing.EASE_OUT_QUAD);

            if (parent == null) {
                // the panel opens out of the icon: it starts as the icon's own box and invisible, then grows.
                // Expo front loads the move, so the content is only stretched for the first fraction of it.
                anims().add(Anims.timeline(AnimEvent.ADDED, OPEN, OPEN.track(0f, 1f), (props, progress) -> {
                    props.scaleX = Anim.lerp(ICON_SIZE / (float) width, 1f, progress);
                    props.scaleY = Anim.lerp(ICON_SIZE / (float) height, 1f, progress);
                    props.alpha = progress;
                }));
                // opened before the panel itself: the manager keeps a parent under its own descendants whatever
                // order they were added in
                manager.open(new IconOverlay(this, width, height));
            } else {
                anims().add(Anims.slideIn(AnimProps.Writer.OFFSET_X, -24f, 250, Easing.EASE_OUT_QUAD))
                        .add(Anims.fadeIn(250, Easing.EASE_OUT_QUAD));
            }
            anims().add(Anims.timeline(AnimEvent.ADDED, wobble, 500, false, false, AnimProps.Writer.OFFSET_Y))
                    .add(Anims.fadeOut(200, Easing.EASE_IN_QUAD))
                    .add(Anims.shrinkOut(200, Easing.EASE_IN_QUAD));

            elements.add(new InputFieldElement(4, TITLE_HEIGHT + 4, () -> String.valueOf(inputFieldRes), a -> inputFieldRes += a))
                    .add(new DemoLabel("label element", 4, 68))
                    .add(new DemoButton(4, 48))
                    .add(new SlotElement(70, 48, () -> DEMO_STACK, () -> List.of(Component.literal("a demo slot"))))
                    .add(new GhostSlotElement(110, 48, () -> ghost, stack -> ghost = stack))
                    .add(ResourceGaugeElement.ofNoSprite(88, 32, 8, 48, UiTextures.CHRONON_CHANNEL,
                            () -> gaugeRatio, 0xFFFFFFFF, () -> List.of(Component.literal(String.valueOf(gaugeRatio)))));
            if (spawnsChild)
                elements.add(new TextureButtonElement(childButtonLeft(), childButtonTop(), () -> Component.literal("open child"),
                        () -> manager.open(new DemoPanel(manager, "child", this, 0, 0, 150, 76, true))));
        }

        @Override
        public void init(Minecraft mc, int screenWidth, int screenHeight) {
            if (parent != null) setPosition(parent.getX() + 20, parent.getY() + 20);
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
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF202020);
            graphics.fill(0, 0, width, TITLE_HEIGHT, manager.isFocused(this) ? 0xFF3A3A8C : 0xFF2A2A50);
            graphics.drawString(Minecraft.getInstance().font, title, 4, 2, 0xFFFFFF);
            if (spawnsChild) {
                graphics.fill(childButtonLeft(), childButtonTop(), childButtonLeft() + 60, childButtonTop() + 12, 0xFF505050);
                graphics.drawString(Minecraft.getInstance().font, "open child", childButtonLeft() + 4, childButtonTop() + 2, 0xFFFFFF);
            }

            // For test: the ratio should not be calced in the render thread.
            if (!reversed) gaugeRatio += .001f;
            else gaugeRatio -= .001f;
            if (gaugeRatio >= 1f || gaugeRatio <= 0f) {
                reversed = !reversed;
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

//        @Override
//        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typed.isEmpty()) {
//                typed.deleteCharAt(typed.length() - 1);
//                return true;
//            }
//            return super.keyPressed(keyCode, scanCode, modifiers);
//        }

        private int childButtonLeft() { return 4; }

        private int childButtonTop() { return TITLE_HEIGHT + 20; }
    }

    /** Fades a private value of its own: the element only reads the handle, the tween lives in the list. */
    static class DemoLabel extends UiElement {
        private final String text;
        private final AnimValue fade = new AnimValue(0f);

        DemoLabel(String text, int x, int y) {
            super(x, y, 76,10);
            this.text = text;
            anims().add(Anims.tween(AnimEvent.ADDED, fade, 0f, 1f, 400, Easing.EASE_OUT_QUAD));
            // a looping track, so it never finishes and can never hold a close back
            anims().add(Anims.pulse(AnimProps.Writer.OVERLAY, 0f, 0.18f, 900, Easing.EASE_IN_OUT_SINE));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float a = fade.getValue();
            graphics.fill(0, 0, getWidth(), getHeight(), (int) (a * 0x40) << 24 | 0xFFFFFF);
            graphics.drawString(Minecraft.getInstance().font, text, 2, 1, (int) (a * 255f) << 24 | 0xFFFFFF);
        }
    }

    /** Hover scales it, clicking spins it: two independent animations, the hit box keeps matching the drawing. */
    static class DemoButton extends UiElement {
        private final Animation spin = Anims.manual(AnimProps.Writer.ROTATION, 0f, (float) (Math.PI / 2), 250, Easing.EASE_IN_OUT_QUAD);
        private boolean stretched;
        private int clicks;

        DemoButton(int x, int y) {
            super(x, y, 60, 14);
            anims().add(Anims.tween(AnimEvent.HOVER, AnimProps.Writer.SCALE_XY, 1f, 1.15f, 150, Easing.EASE_OUT_QUAD));
            anims().add(spin);
        }

        @Override
        public boolean isMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            clicks++;
            stretched = !stretched;
            if (stretched) spin.play();
            else spin.reverse();
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, getWidth(), getHeight(), stretched ? 0xFF7A3A3A : 0xFF3A5A3A);
            graphics.drawString(Minecraft.getInstance().font, "clicks " + clicks, 2, 3, 0xFFFFFF);
        }
    }

    /**
     * One block of the intro icon: it turns with its twin, then slides to its own corner of the panel.
     * The texture is drawn 1:1 from its sub rectangle, the whole move is a translation.
     * {@code u / v} is both this block's offset inside the icon and where its pixels start; {@code targetX / targetY}
     * is where it lands, in the overlay's own space.
     */
    static class IconBlock extends UiElement {
        private final int u;
        private final int v;

        IconBlock(int x, int y, int u, int v, float centreX, float centreY, int targetX, int targetY) {
            super(x, y, ICON_BLOCK, ICON_BLOCK);
            this.u = u;
            this.v = v;
            // both blocks turn around the icon's centre, so each pivot is where that centre falls inside its own box
            AnimProps props = anims().props();
            props.pivotX = (centreX - x) / ICON_BLOCK;
            props.pivotY = (centreY - y) / ICON_BLOCK;

            anims().add(Anims.timeline(AnimEvent.ADDED, SPIN, SPIN.track(0f, TURNS), AnimProps.Writer.ROTATION));
            // the very same cue the panel opens on, so the two stay in step by construction
            anims().add(Anims.timeline(AnimEvent.ADDED, OPEN, OPEN.track(0f, 1f), (p, progress) -> {
                p.offsetX = (targetX - x) * progress;
                p.offsetY = (targetY - y) * progress;
            }));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(UiTextures.FRONT_ICON, 0, 0, ICON_BLOCK, ICON_BLOCK, u, v, ICON_BLOCK, ICON_BLOCK,
                    ICON_SIZE, ICON_SIZE);
        }
    }

    /**
     * Hosts the intro blocks: a window of its own, so the panel's pose cannot scale them, while still being part of
     * the panel's tree. It takes no input at all, which is what lets it sit on top of the panel it decorates.
     */
    static class IconOverlay extends AbstractUiWindow {
        private final IUiWindow parent;

        IconOverlay(IUiWindow parent, int panelWidth, int panelHeight) {
            this.parent = parent;

            int iconLeft = (panelWidth - ICON_SIZE) / 2;
            int iconTop = (panelHeight - ICON_SIZE) / 2;
            int inset = ICON_SIZE - ICON_BLOCK;
            float centreX = iconLeft + ICON_SIZE / 2f;
            float centreY = iconTop + ICON_SIZE / 2f;
            elements.add(new IconBlock(iconLeft, iconTop, 0, 0, centreX, centreY, 0, 0));
            elements.add(new IconBlock(iconLeft + inset, iconTop + inset, inset, inset, centreX, centreY,
                    panelWidth - ICON_BLOCK, panelHeight - ICON_BLOCK));
        }

        @Override
        public IUiWindow getParent() {
            return parent;
        }

        /** Decoration only: it never becomes the hovered or clicked node, so the panel underneath keeps working. */
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return false;
        }

        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

        @Override
        public int getX() { return parent.getX(); }

        @Override
        public int getY() { return parent.getY(); }

        @Override
        public int getWidth() { return parent.getWidth(); }

        @Override
        public int getHeight() { return parent.getHeight(); }

        /** The position is derived from the parent, there is nothing of its own to store. */
        @Override
        public void setPosition(int x, int y) {}
    }
}
