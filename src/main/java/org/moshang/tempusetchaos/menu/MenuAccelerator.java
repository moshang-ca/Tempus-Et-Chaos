package org.moshang.tempusetchaos.menu;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;
import org.moshang.tempusetchaos.registry.TECMenus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Blacklist slots are virtual: they hold nothing, they only mirror the accelerator's blacklist so the entries show up
 * as items, and clicks on them add or remove an entry instead of moving items around.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MenuAccelerator extends AbstractContainerMenu {
    public static final int DATA_MULTIPLIER = 0;
    public static final int DATA_CONSUMED = 1;
    public static final int DATA_CHRONON = 2;
    public static final int DATA_CAPACITY = 3;
    public static final int DATA_COUNT = 4;

    public static final int BUTTON_DECREASE = 0;
    public static final int BUTTON_INCREASE = 1;

    public static final int BLACKLIST_COLUMNS = 4;
    public static final int BLACKLIST_ROWS = 2;
    public static final int BLACKLIST_SIZE = BLACKLIST_COLUMNS * BLACKLIST_ROWS;
    public static final int BLACKLIST_X = 22;
    public static final int BLACKLIST_Y = 35;

    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 84;
    public static final int HOTBAR_Y = 142;

    private final Container blacklistView;
    private final ContainerData data;
    @Nullable
    private final BEAccelerator accelerator;
    @Nullable
    private final BlacklistView serverView;

    /** Client side, filled by the slot and data sync packets. */
    public MenuAccelerator(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(BLACKLIST_SIZE), new SimpleContainerData(DATA_COUNT), null);
    }

    /** Server side. */
    public MenuAccelerator(int containerId, Inventory playerInventory, BEAccelerator accelerator) {
        this(containerId, playerInventory, new BlacklistView(accelerator), acceleratorData(accelerator), accelerator);
    }

    private MenuAccelerator(int containerId, Inventory playerInventory, Container blacklistView, ContainerData data,
                            @Nullable BEAccelerator accelerator) {
        super(TECMenus.ACCELERATOR_MENU.get(), containerId);
        this.blacklistView = blacklistView;
        this.data = data;
        this.accelerator = accelerator;
        this.serverView = blacklistView instanceof BlacklistView view ? view : null;
        if (serverView != null) serverView.refresh();
        checkContainerSize(blacklistView, BLACKLIST_SIZE);
        checkContainerDataCount(data, DATA_COUNT);

        for (int i = 0; i < BLACKLIST_SIZE; i++) {
            addSlot(new ReadOnlySlot(blacklistView, i,
                    BLACKLIST_X + i % BLACKLIST_COLUMNS * 18,
                    BLACKLIST_Y + i / BLACKLIST_COLUMNS * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public int getMultiplier() {
        return data.get(DATA_MULTIPLIER);
    }

    public int getConsumed() {
        return data.get(DATA_CONSUMED);
    }

    public int getChrononStored() {
        return data.get(DATA_CHRONON);
    }

    public int getChrononCapacity() {
        return data.get(DATA_CAPACITY);
    }

    @Override
    public void broadcastChanges() {
        if (serverView != null) serverView.refresh();
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (serverView != null && slotId >= 0 && slotId < BLACKLIST_SIZE) {
            toggleBlacklist(slotId);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void toggleBlacklist(int slot) {
        if (accelerator == null || serverView == null) return;
        ItemStack carried = getCarried();
        if (!carried.isEmpty()) {
            // only blocks that have an item can be listed, that is also what the accelerator can check against
            Block block = Block.byItem(carried.getItem());
            if (block == Blocks.AIR) return;
            accelerator.addBlacklist(BuiltInRegistries.BLOCK.getKey(block));
        } else {
            ResourceLocation entry = serverView.entryAt(slot);
            if (entry == null) return;
            accelerator.removeBlacklist(entry);
        }
        serverView.refresh();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (accelerator == null) return false;
        int step = switch (id) {
            case BUTTON_DECREASE -> -1;
            case BUTTON_INCREASE -> 1;
            default -> 0;
        };
        if (step == 0) return false;
        accelerator.setAccelerateMultiplier(accelerator.getAccelerateMultiplier() + step);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BLACKLIST_SIZE) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        int inventoryStart = BLACKLIST_SIZE;
        int hotbarStart = inventoryStart + 27;
        boolean success = index < hotbarStart
                ? moveItemStackTo(stack, hotbarStart, hotbarStart + 9, false)
                : moveItemStackTo(stack, inventoryStart, hotbarStart, false);
        if (!success) return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return accelerator == null || Container.stillValidBlockEntity(accelerator, player);
    }

    private static ContainerData acceleratorData(BEAccelerator accelerator) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_MULTIPLIER -> accelerator.getAccelerateMultiplier();
                    case DATA_CONSUMED -> accelerator.getConsumed();
                    case DATA_CHRONON -> Math.clamp(accelerator.getChrononStored(), 0, Integer.MAX_VALUE);
                    case DATA_CAPACITY -> Math.clamp(accelerator.getChrononCapacity(), 0, Integer.MAX_VALUE);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static class ReadOnlySlot extends Slot {
        ReadOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Server side view of the blacklist, refreshed once per tick before the slots are broadcast. */
    private static class BlacklistView implements Container {
        private final BEAccelerator accelerator;
        private List<ResourceLocation> entries = List.of();
        private List<ItemStack> view = List.of();

        BlacklistView(BEAccelerator accelerator) {
            this.accelerator = accelerator;
        }

        void refresh() {
            List<ResourceLocation> ids = accelerator.getBlacklist().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
            List<ResourceLocation> shown = new ArrayList<>(BLACKLIST_SIZE);
            List<ItemStack> stacks = new ArrayList<>(BLACKLIST_SIZE);
            for (ResourceLocation id : ids) {
                if (shown.size() >= BLACKLIST_SIZE) break;
                Item item = BuiltInRegistries.BLOCK.get(id).asItem();
                if (item == Items.AIR) continue;
                shown.add(id);
                stacks.add(item.getDefaultInstance());
            }
            entries = shown;
            view = stacks;
        }

        @Nullable
        ResourceLocation entryAt(int slot) {
            return slot >= 0 && slot < entries.size() ? entries.get(slot) : null;
        }

        @Override
        public int getContainerSize() {
            return BLACKLIST_SIZE;
        }

        @Override
        public boolean isEmpty() {
            return view.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < view.size() ? view.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {}

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {}
    }
}
