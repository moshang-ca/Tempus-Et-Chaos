package org.moshang.tempusetchaos.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.client.gui.element.UiElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A window that shows a container menu. The slots live in the window's own space, so they move, stack and animate
 * with it exactly like the elements do, and the menu itself stays owned by the screen and is handed over here.
 * <p>
 * The window drives the whole slot interaction itself, in its own local coordinates: the press state machine, the
 * carried stack, the spread drag and the touch paths all belong to this object.
 * <p>
 * Grabbing a spot that is not a slot drags the window, so the frame of the window is its own title bar.
 * <p>
 * Do not animate {@link org.moshang.tempusetchaos.client.gui.anim.AnimProps.Writer#OVERLAY} on a container window:
 * the wash lands on top of everything the window draws, the carried item included. Use the alpha writer instead.
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public abstract class ContainerWindow<T extends AbstractContainerMenu> extends AbstractUiWindow {
    protected static final int SLOT_SIZE = 16;

    protected final T menu;
    protected final Minecraft mc = Minecraft.getInstance();

    @Getter
    private int x;
    @Getter
    private int y;
    @Getter
    private int width;
    @Getter
    private int height;
    private boolean centered;

    @Nullable
    protected Slot hoveredSlot;

    /** Touch only: the slot the drag started on, and the stack lifted off it. */
    @Nullable
    private Slot clickedSlot;
    private ItemStack draggingItem = ItemStack.EMPTY;
    private boolean isSplittingStack;
    @Nullable
    private Slot quickdropSlot;
    private long quickdropTime;

    /** Touch only: the drawn trip of a released stack back to its slot. */
    @Nullable
    private Slot snapbackEnd;
    private ItemStack snapbackItem = ItemStack.EMPTY;
    private int snapbackStartX;
    private int snapbackStartY;
    private long snapbackTime;

    /** The plain drag that spreads the carried stack over the slots it passes. */
    private final Set<Slot> quickCraftSlots = new LinkedHashSet<>();
    private boolean isQuickCrafting;
    private int quickCraftingType;
    private int quickCraftingButton;
    private int quickCraftingRemainder;

    private boolean skipNextRelease = true;
    private boolean doubleClick;
    @Nullable
    private Slot lastClickSlot;
    private long lastClickTime;
    private int lastClickButton;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    protected ContainerWindow(T menu, int width, int height) {
        this.menu = menu;
        this.width = width;
        this.height = height;
    }

    @Override
    public void init(Minecraft mc, int screenWidth, int screenHeight) {
        // the window carries its own geometry, so the first layout is the only one that places it
        if (!centered) {
            x = (screenWidth - width) / 2;
            y = (screenHeight - height) / 2;
            centered = true;
        }
        super.init(mc, screenWidth, screenHeight);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean handlesOutsideClicks() {
        return true;
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);

        // the slots sit above the window art, the labels and the elements come back down over them
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0f, 0f, AbstractContainerScreen.SLOT_ITEM_BLIT_OFFSET);

        hoveredSlot = null;
        for (Slot slot : menu.slots) {
            if (slot.isActive()) renderSlot(graphics, slot);
            if (slot.isActive() && isHovering(slot, mouseX, mouseY)) {
                hoveredSlot = slot;
                renderSlotHighlight(graphics, slot);
            }
        }

        pose.popPose();
        renderLabels(graphics, mouseX, mouseY);
    }

    /**
     * The stack on the cursor, drawn after every window by the host. This runs unclipped and unposed with the mouse
     * in host space, so the offsets are the plain vanilla ones and the stack can never end up under a window.
     */
    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ItemStack carried = menu.getCarried();
        if (!draggingItem.isEmpty()) {
            ItemStack stack = isSplittingStack ? draggingItem.copyWithCount(Mth.ceil(draggingItem.getCount() / 2f)) : draggingItem;
            renderFloatingItem(graphics, stack, mouseX - 8, mouseY - 16, null);
        } else if (!carried.isEmpty()) {
            ItemStack stack = carried;
            String count = null;
            if (isQuickCrafting && quickCraftSlots.size() > 1) {
                stack = carried.copyWithCount(quickCraftingRemainder);
                if (stack.isEmpty()) count = ChatFormatting.YELLOW + "0";
            }
            renderFloatingItem(graphics, stack, mouseX - 8, mouseY - 8, count);
        }

        if (!snapbackItem.isEmpty() && snapbackEnd != null) {
            float progress = Math.min((Util.getMillis() - snapbackTime) / 100f, 1f);
            if (progress >= 1f) snapbackItem = ItemStack.EMPTY;
            int dx = snapbackEnd.x - snapbackStartX;
            int dy = snapbackEnd.y - snapbackStartY;
            renderFloatingItem(graphics, snapbackItem, snapbackStartX + (int) (dx * progress),
                    snapbackStartY + (int) (dy * progress), null);
        }
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        UiElement hovered = elements.getHovered();
        if (hovered != null && !hovered.getTooltip().isEmpty()) {
            super.renderTooltips(graphics, mouseX, mouseY);
            return;
        }
        if (!menu.getCarried().isEmpty() || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        ItemStack stack = hoveredSlot.getItem();
        graphics.renderTooltip(mc.font, Screen.getTooltipFromItem(mc, stack), stack.getTooltipImage(), stack, mouseX, mouseY);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = findSlot(mouseX, mouseY);
        // an empty hand on the frame is a drag handle: the press is claimed here so the host starts the framework
        // drag, the move itself is the host's job. The flag gates whether the host may drag at all.
        if (button == 0 && slot == null && !hasClickedOutside(mouseX, mouseY)) return canDrag;

        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
        assert mc.player != null;
        boolean pick = mc.options.keyPickItem.isActiveAndMatches(mouseKey) && mc.player.hasInfiniteMaterials();
        long now = Util.getMillis();
        doubleClick = lastClickSlot == slot && now - lastClickTime < 250L && lastClickButton == button;
        skipNextRelease = false;

        if (button != 0 && button != 1 && !pick) {
            checkHotbarMouseClicked(button);
        } else {
            boolean outside = slot == null && hasClickedOutside(mouseX, mouseY);
            // a click that lands on neither a slot nor a carried stack belongs to whatever sits behind this window
            if (outside && menu.getCarried().isEmpty()) return false;
            int slotId = slot != null ? slot.index : (outside ? -999 : -1);
            if (slotId != -1) {
                if (mc.options.touchscreen().get()) {
                    if (slot != null && slot.hasItem()) {
                        clickedSlot = slot;
                        draggingItem = ItemStack.EMPTY;
                        isSplittingStack = button == 1;
                    } else {
                        clickedSlot = null;
                    }
                } else if (!isQuickCrafting) {
                    if (menu.getCarried().isEmpty()) {
                        if (pick) {
                            slotClicked(slot, slotId, button, ClickType.CLONE);
                        } else {
                            boolean quickMove = slotId != -999 && isQuickMoveKeyDown();
                            ClickType type = ClickType.PICKUP;
                            if (quickMove) {
                                lastQuickMoved = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                type = ClickType.QUICK_MOVE;
                            } else if (slotId == -999) {
                                type = ClickType.THROW;
                            }
                            slotClicked(slot, slotId, button, type);
                        }
                        skipNextRelease = true;
                    } else {
                        // carrying something: this press starts a spread, the release commits it
                        isQuickCrafting = true;
                        quickCraftingButton = button;
                        quickCraftSlots.clear();
                        if (button == 0) quickCraftingType = 0;
                        else if (button == 1) quickCraftingType = 1;
                        else quickCraftingType = 2;
                    }
                }
            }
        }

        lastClickSlot = slot;
        lastClickTime = now;
        lastClickButton = button;
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) {
        Slot slot = findSlot(mouseX, mouseY);
        boolean outside = slot == null && hasClickedOutside(mouseX, mouseY);
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
        int slotId = slot != null ? slot.index : (outside ? -999 : -1);

        if (doubleClick && slot != null && button == 0 && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (Screen.hasShiftDown()) {
                if (!lastQuickMoved.isEmpty()) {
                    for (Slot other : menu.slots) {
                        assert mc.player != null;
                        if (other.mayPickup(mc.player) && other.hasItem() && other.isSameInventory(slot)
                                && AbstractContainerMenu.canItemQuickReplace(other, lastQuickMoved, true)) {
                            slotClicked(other, other.index, button, ClickType.QUICK_MOVE);
                        }
                    }
                }
            } else {
                slotClicked(slot, slotId, button, ClickType.PICKUP_ALL);
            }
            doubleClick = false;
            lastClickTime = 0L;
        } else {
            if (isQuickCrafting && quickCraftingButton != button) {
                isQuickCrafting = false;
                quickCraftSlots.clear();
                skipNextRelease = true;
                return true;
            }
            if (skipNextRelease) {
                skipNextRelease = false;
                return true;
            }
            if (clickedSlot != null && mc.options.touchscreen().get()) {
                if (button == 0 || button == 1) {
                    if (draggingItem.isEmpty() && slot != clickedSlot) draggingItem = clickedSlot.getItem();
                    boolean canReplace = slot != null && !draggingItem.isEmpty()
                            && AbstractContainerMenu.canItemQuickReplace(slot, draggingItem, false);
                    if (slotId != -1 && !draggingItem.isEmpty() && canReplace) {
                        slotClicked(clickedSlot, clickedSlot.index, button, ClickType.PICKUP);
                        slotClicked(slot, slotId, 0, ClickType.PICKUP);
                        if (menu.getCarried().isEmpty()) {
                            snapbackItem = ItemStack.EMPTY;
                        } else {
                            slotClicked(clickedSlot, clickedSlot.index, button, ClickType.PICKUP);
                            beginSnapback(mouseX, mouseY);
                        }
                    } else if (!draggingItem.isEmpty()) {
                        beginSnapback(mouseX, mouseY);
                    }
                    clearDraggingState();
                }
            } else if (isQuickCrafting && !quickCraftSlots.isEmpty()) {
                slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, quickCraftingType), ClickType.QUICK_CRAFT);
                for (Slot target : quickCraftSlots) {
                    slotClicked(target, target.index, AbstractContainerMenu.getQuickcraftMask(1, quickCraftingType),
                            ClickType.QUICK_CRAFT);
                }
                slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, quickCraftingType), ClickType.QUICK_CRAFT);
            } else if (!menu.getCarried().isEmpty()) {
                if (mc.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                    slotClicked(slot, slotId, button, ClickType.CLONE);
                } else {
                    boolean quickMove = slotId != -999 && isQuickMoveKeyDown();
                    if (quickMove) lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    slotClicked(slot, slotId, button, quickMove ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            } else {
                return false;
            }
        }

        if (menu.getCarried().isEmpty()) lastClickTime = 0L;
        isQuickCrafting = false;
        return true;
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Slot slot = findSlot(mouseX, mouseY);
        ItemStack carried = menu.getCarried();

        if (clickedSlot != null && mc.options.touchscreen().get()) {
            if (button != 0 && button != 1) return true;
            if (draggingItem.isEmpty()) {
                if (slot != clickedSlot && !clickedSlot.getItem().isEmpty()) draggingItem = clickedSlot.getItem().copy();
            } else if (draggingItem.getCount() > 1 && slot != null
                    && AbstractContainerMenu.canItemQuickReplace(slot, draggingItem, false)) {
                long now = Util.getMillis();
                if (quickdropSlot == slot) {
                    if (now - quickdropTime > 500L) {
                        slotClicked(clickedSlot, clickedSlot.index, 0, ClickType.PICKUP);
                        slotClicked(slot, slot.index, 1, ClickType.PICKUP);
                        slotClicked(clickedSlot, clickedSlot.index, 0, ClickType.PICKUP);
                        quickdropTime = now + 750L;
                        draggingItem.shrink(1);
                    }
                } else {
                    quickdropSlot = slot;
                    quickdropTime = now;
                }
            }
            return true;
        }

        if (isQuickCrafting && slot != null && !carried.isEmpty()
                && (carried.getCount() > quickCraftSlots.size() || quickCraftingType == 2)
                && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
                && slot.mayPlace(carried) && menu.canDragTo(slot)) {
            quickCraftSlots.add(slot);
            recalculateQuickCraftRemaining();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        boolean handled = checkHotbarKeyPressed(keyCode, scanCode);
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            if (mc.options.keyPickItem.isActiveAndMatches(mouseKey)) {
                slotClicked(hoveredSlot, hoveredSlot.index, 0, ClickType.CLONE);
                handled = true;
            } else if (mc.options.keyDrop.isActiveAndMatches(mouseKey)) {
                slotClicked(hoveredSlot, hoveredSlot.index, Screen.hasControlDown() ? 1 : 0, ClickType.THROW);
                handled = true;
            }
        } else if (mc.options.keyDrop.isActiveAndMatches(mouseKey)) {
            handled = true;
        }
        return handled;
    }

    /** The window art, drawn at the window origin: the host has already moved the pose here. */
    protected abstract void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);

    /** Drawn over the slots, at the window origin. Empty by default. */
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        int sx = slot.x;
        int sy = slot.y;
        ItemStack item = slot.getItem();
        boolean spread = false;
        boolean hidden = slot == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
        ItemStack carried = menu.getCarried();
        String count = null;

        if (slot == clickedSlot && !draggingItem.isEmpty() && isSplittingStack && !item.isEmpty()) {
            item = item.copyWithCount(item.getCount() / 2);
        } else if (isQuickCrafting && quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (quickCraftSlots.size() == 1) return;
            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && menu.canDragTo(slot)) {
                spread = true;
                int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int inSlot = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int target = AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, carried) + inSlot;
                if (target > max) {
                    target = max;
                    count = ChatFormatting.YELLOW.toString() + max;
                }
                item = carried.copyWithCount(target);
            } else {
                quickCraftSlots.remove(slot);
                recalculateQuickCraftRemaining();
            }
        }

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0f, 0f, AbstractContainerScreen.SLOT_ITEM_BLIT_OFFSET);

        if (item.isEmpty() && slot.isActive()) {
            Pair<ResourceLocation, ResourceLocation> icon = slot.getNoItemIcon();
            if (icon != null) {
                TextureAtlasSprite sprite = mc.getTextureAtlas(icon.getFirst()).apply(icon.getSecond());
                graphics.blit(sx, sy, 0, SLOT_SIZE, SLOT_SIZE, sprite);
                hidden = true;
            }
        }

        if (!hidden) {
            if (spread) graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0x80FFFFFF);
            renderSlotContents(graphics, item, slot, count);
        }

        pose.popPose();
    }

    protected void renderSlotContents(GuiGraphics graphics, ItemStack item, Slot slot, @Nullable String count) {
        int sx = slot.x;
        int sy = slot.y;
        int seed = sx + sy * width;
        if (slot.isFake()) graphics.renderFakeItem(item, sx, sy, seed);
        else graphics.renderItem(item, sx, sy, seed);
        graphics.renderItemDecorations(mc.font, item, sx, sy, count);
    }

    protected void renderSlotHighlight(GuiGraphics graphics, Slot slot) {
        if (!slot.isHighlightable()) return;
        int color = slotColor(slot.index);
        graphics.fillGradient(RenderType.guiOverlay(), slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE,
                color, color, 0);
    }

    protected int slotColor(int index) {
        return 0x80FFFFFF;
    }

    protected void slotClicked(@Nullable Slot slot, int slotId, int button, ClickType type) {
        if (mc.player == null || mc.gameMode == null) return;
        int index = slot != null ? slot.index : slotId;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, index, button, type, mc.player);
    }

    /** Slot boxes are hit tested with a one pixel grace all around, the same as vanilla. */
    protected boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return mouseX >= slot.x - 1 && mouseX < slot.x + SLOT_SIZE + 1
                && mouseY >= slot.y - 1 && mouseY < slot.y + SLOT_SIZE + 1;
    }

    @Nullable
    protected Slot findSlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot, mouseX, mouseY)) return slot;
        }
        return null;
    }

    protected void clearDraggingState() {
        draggingItem = ItemStack.EMPTY;
        clickedSlot = null;
    }

    private void beginSnapback(double mouseX, double mouseY) {
        snapbackStartX = Mth.floor(mouseX);
        snapbackStartY = Mth.floor(mouseY);
        snapbackEnd = clickedSlot;
        snapbackItem = draggingItem;
        snapbackTime = Util.getMillis();
    }

    private void renderFloatingItem(GuiGraphics graphics, ItemStack stack, int x, int y, @Nullable String count) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0f, 0f, 232f);
        graphics.renderItem(stack, x, y);
        var font = IClientItemExtensions.of(stack).getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT);
        graphics.renderItemDecorations(font == null ? mc.font : font, stack, x, y, count);
        pose.popPose();
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || !isQuickCrafting) return;
        if (quickCraftingType == 2) {
            quickCraftingRemainder = carried.getMaxStackSize();
            return;
        }
        quickCraftingRemainder = carried.getCount();
        for (Slot slot : quickCraftSlots) {
            ItemStack inSlot = slot.getItem();
            int have = inSlot.isEmpty() ? 0 : inSlot.getCount();
            int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
            int place = AbstractContainerMenu.getQuickCraftPlaceCount(quickCraftSlots, quickCraftingType, carried);
            quickCraftingRemainder -= Math.min(place + have, max) - have;
        }
    }

    private void checkHotbarMouseClicked(int button) {
        if (hoveredSlot == null || !menu.getCarried().isEmpty()) return;
        if (mc.options.keySwapOffhand.matchesMouse(button)) {
            slotClicked(hoveredSlot, hoveredSlot.index, 40, ClickType.SWAP);
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.options.keyHotbarSlots[i].matchesMouse(button)) {
                slotClicked(hoveredSlot, hoveredSlot.index, i, ClickType.SWAP);
            }
        }
    }

    private boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        if (!menu.getCarried().isEmpty() || hoveredSlot == null) return false;
        if (mc.options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            slotClicked(hoveredSlot, hoveredSlot.index, 40, ClickType.SWAP);
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                slotClicked(hoveredSlot, hoveredSlot.index, i, ClickType.SWAP);
                return true;
            }
        }
        return false;
    }

    private boolean hasClickedOutside(double mouseX, double mouseY) {
        return mouseX < 0 || mouseY < 0 || mouseX >= width || mouseY >= height;
    }

    private boolean isQuickMoveKeyDown() {
        long window = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(window, 340) || InputConstants.isKeyDown(window, 344);
    }

    /**
     * Releasing the container belongs to the close, not to being dropped from the host: a screen that lays itself
     * out again takes its windows out and puts them back, and the menu has to survive that.
     */
    @Override
    public void onClose() {
        super.onClose();
        if (mc.player != null && mc.getConnection() != null && mc.player.containerMenu == menu) {
            mc.player.closeContainer();
        }
    }
}
