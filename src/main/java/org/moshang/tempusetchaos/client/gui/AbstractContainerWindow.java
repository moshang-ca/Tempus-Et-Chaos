package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class AbstractContainerWindow<T extends AbstractContainerMenu> extends AbstractUiWindow {
    protected final T menu;
    private final Host host;

    @Nullable
    private Minecraft mc;
    private boolean dragging;
    private double grabX;
    private double grabY;
    private float partialTick;

    protected AbstractContainerWindow(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        this.menu = menu;
        this.host = new Host(menu, playerInventory, title, imageWidth, imageHeight);
    }

    @Override
    public void init(Minecraft mc, int screenWidth, int screenHeight) {
        this.mc = mc;
        host.bind(mc, screenWidth, screenHeight);
        super.init(mc, screenWidth, screenHeight);
    }

    @Override
    public void onResize(int screenWidth, int screenHeight) {
        if (mc != null) host.bind(mc, screenWidth, screenHeight);
        super.onResize(screenWidth, screenHeight);
    }

    @Override
    public void tick() {
        super.tick();
        if (mc != null) host.tick();
    }

    @Override
    public void onClose() {
        super.onClose();
        dispose();
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
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
    public boolean handlesOutsideClicks() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderContent(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.partialTick = partialTick;
        host.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        host.renderFloatingItem(graphics, mouseX - 8, mouseY - 8);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        // any spot that is not a slot is a drag handle, so the image stays exactly where vanilla puts it
        if (button == 0 && isMouseOver(mouseX, mouseY) && !host.overSlot(mouseX, mouseY)) {
            dragging = true;
            grabX = mouseX;
            grabY = mouseY;
            return true;
        }
        return host.handleClick(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return host.handleRelease(mouseX, mouseY, button);
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return false;
        setPosition((int) Math.round(getX() + (mouseX - grabX)), (int) Math.round(getY() + (mouseY - grabY)));
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (mc != null && mc.options.keyInventory.matches(keyCode, scanCode)) return false;
        return host.handleKey(keyCode, scanCode);
    }

    protected int leftPos() {
        return host.localX();
    }

    protected int topPos() {
        return host.localY();
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

    @Override
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        UiElement hovered = elements.getHovered();
        if (hovered != null && !hovered.getTooltip().isEmpty()) {
            super.renderTooltips(graphics, mouseX, mouseY);
        } else {
            host.renderSlotTooltip(graphics, mouseX, mouseY);
        }
    }

    private void dispose() {
        if (mc != null && mc.player != null && mc.player.containerMenu == menu && mc.getConnection() != null) {
            mc.player.closeContainer();
        }
        host.removed();
    }

    @SuppressWarnings("DataFlowIssue")
    private final class Host extends AbstractContainerScreen<T> {
        private int posX;
        private int posY;
        private boolean placed;

        private boolean skipNextRelease = true;
        private boolean doubleClick;
        @Nullable
        private Slot lastClickSlot;
        private long lastClickTime;
        private int lastClickButton;
        private ItemStack lastQuickMoved = ItemStack.EMPTY;

        Host(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
            super(menu, playerInventory, title);
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        void bind(Minecraft mc, int screenWidth, int screenHeight) {
            this.minecraft = mc;
            this.font = mc.font;
            this.width = screenWidth;
            this.height = screenHeight;
            if (!placed) {
                posX = (screenWidth - imageWidth) / 2;
                posY = (screenHeight - imageHeight) / 2;
                placed = true;
            }
            applyPosition();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBg(graphics, partialTick, mouseX, mouseY);
            RenderSystem.disableDepthTest();
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos, topPos, 0f);
            hoveredSlot = null;
            for (Slot slot : menu.slots) {
                if (slot.isActive()) renderSlot(graphics, slot);
                if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    hoveredSlot = slot;
                    renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
                }
            }
            renderLabels(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }

        /** The host draws this after every window, so the item on the cursor never ends up under one of them. */
        void renderFloatingItem(GuiGraphics graphics, int x, int y) {
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) return;
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 232f);
            graphics.renderItem(carried, x, y);
            var font = IClientItemExtensions.of(carried).getFont(carried, IClientItemExtensions.FontContext.ITEM_COUNT);
            graphics.renderItemDecorations(font == null ? this.font : font, carried, x, y);
            graphics.pose().popPose();
        }

        /** Vanilla {@code mouseClicked} without the super call that broadcasts the click. */
        boolean handleClick(double mouseX, double mouseY, int button) {
            assert  minecraft != null;
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
            boolean pick = minecraft.options.keyPickItem.isActiveAndMatches(mouseKey) && minecraft.player.hasInfiniteMaterials();
            Slot slot = findSlot(mouseX, mouseY);
            long now = Util.getMillis();
            this.doubleClick = this.lastClickSlot == slot && now - this.lastClickTime < 250L && this.lastClickButton == button;
            this.skipNextRelease = false;
            if (button != 0 && button != 1 && !pick) {
                checkHotbarMouseClicked(button);
            } else {
                boolean outside = slot == null && hasClickedOutside(mouseX, mouseY, leftPos, topPos, button);
                // a click on neither a slot nor a carried item belongs to whatever sits behind this window
                if (outside && menu.getCarried().isEmpty()) return false;
                int slotId = slot != null ? slot.index : (outside ? -999 : -1);
                // carrying something: vanilla places it on release, which reaches us through the focused window
                if (slotId != -1 && menu.getCarried().isEmpty()) {
                    if (pick) {
                        slotClicked(slot, slotId, button, ClickType.CLONE);
                    } else {
                        boolean quickMove = slotId != -999 && isQuickMoveKeyDown();
                        ClickType type = ClickType.PICKUP;
                        if (quickMove) {
                            this.lastQuickMoved = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                            type = ClickType.QUICK_MOVE;
                        } else if (slotId == -999) {
                            type = ClickType.THROW;
                        }
                        slotClicked(slot, slotId, button, type);
                    }
                    this.skipNextRelease = true;
                }
            }
            this.lastClickSlot = slot;
            this.lastClickTime = now;
            this.lastClickButton = button;
            return true;
        }

        /** Vanilla {@code mouseReleased} without the super call. The quick craft drag state machine is not in yet. */
        boolean handleRelease(double mouseX, double mouseY, int button) {
            Slot slot = findSlot(mouseX, mouseY);
            boolean outside = slot == null && hasClickedOutside(mouseX, mouseY, leftPos, topPos, button);
            int slotId = slot != null ? slot.index : (outside ? -999 : -1);
            if (this.doubleClick && slot != null && button == 0 && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
                if (hasShiftDown()) {
                    if (!this.lastQuickMoved.isEmpty()) {
                        for (Slot other : menu.slots) {
                            if (other.mayPickup(minecraft.player) && other.hasItem() && other.isSameInventory(slot)
                                    && AbstractContainerMenu.canItemQuickReplace(other, this.lastQuickMoved, true)) {
                                slotClicked(other, other.index, button, ClickType.QUICK_MOVE);
                            }
                        }
                    }
                } else {
                    slotClicked(slot, slotId, button, ClickType.PICKUP_ALL);
                }
                this.doubleClick = false;
                this.lastClickTime = 0L;
            } else if (this.skipNextRelease) {
                this.skipNextRelease = false;
                return true;
            } else if (!menu.getCarried().isEmpty()) {
                InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
                if (minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    slotClicked(slot, slotId, button, ClickType.CLONE);
                } else {
                    boolean quickMove = slotId != -999 && isQuickMoveKeyDown();
                    if (quickMove) this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    slotClicked(slot, slotId, button, quickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            } else {
                return false;
            }
            if (menu.getCarried().isEmpty()) this.lastClickTime = 0L;
            return true;
        }

        /** Vanilla {@code keyPressed} without the super call; the close key is the window manager's business. */
        boolean handleKey(int keyCode, int scanCode) {
            InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
            boolean handled = checkHotbarKeyPressed(keyCode, scanCode);
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                if (minecraft.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    slotClicked(hoveredSlot, hoveredSlot.index, 0, ClickType.CLONE);
                    handled = true;
                } else if (minecraft.options.keyDrop.isActiveAndMatches(mouseKey)) {
                    slotClicked(hoveredSlot, hoveredSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
                    handled = true;
                }
            } else if (minecraft.options.keyDrop.isActiveAndMatches(mouseKey)) {
                handled = true;
            }
            return handled;
        }

        @Nullable
        private Slot findSlot(double mouseX, double mouseY) {
            for (Slot slot : menu.slots) {
                if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) return slot;
            }
            return null;
        }

        private void checkHotbarMouseClicked(int button) {
            if (hoveredSlot == null || !menu.getCarried().isEmpty()) return;
            if (minecraft.options.keySwapOffhand.matchesMouse(button)) {
                slotClicked(hoveredSlot, hoveredSlot.index, 40, ClickType.SWAP);
                return;
            }
            for (int i = 0; i < 9; i++) {
                if (minecraft.options.keyHotbarSlots[i].matchesMouse(button)) {
                    slotClicked(hoveredSlot, hoveredSlot.index, i, ClickType.SWAP);
                }
            }
        }

        private boolean isQuickMoveKeyDown() {
            long window = minecraft.getWindow().getWindow();
            return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            AbstractContainerWindow.this.renderBg(graphics, partialTick, mouseX, mouseY);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            AbstractContainerWindow.this.renderLabels(graphics, mouseX, mouseY);
            AbstractContainerWindow.this.elements.render(graphics, mouseX, mouseY, partialTick);
        }

        void vanillaLabels(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderLabels(graphics, mouseX, mouseY);
        }

        void renderSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            renderTooltip(graphics, mouseX, mouseY);
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

        int localX() {
            return leftPos;
        }

        int localY() {
            return topPos;
        }

        int windowX() {
            return posX;
        }

        int windowY() {
            return posY;
        }

        private void applyPosition() {
            leftPos = 0;
            topPos = 0;
        }
    }
}
