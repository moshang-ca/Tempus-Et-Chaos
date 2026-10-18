package org.moshang.tempusetchaos.menu;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;

import java.util.UUID;

public abstract class ChrononMenu<T extends BaseChrononNodeBlockEntity> extends AbstractContainerMenu {
    @Getter
    protected final T blockEntity;
    @Getter
    protected final BlockPos blockPos;
    @Getter
    protected final int machineSlots;

    protected ChrononMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, T blockEntity) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        blockPos = blockEntity.getBlockPos();

        addMachineSlots();
        machineSlots = slots.size();

        for (int l = 0; l < 3; l++) {
            for (int j1 = 0; j1 < 9; j1++) {
                this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142));
        }
    }

    protected void addMachineSlots() {  }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        boolean moved;
        if (index < machineSlots) {
            moved = moveItemStackTo(stack, machineSlots, slots.size(), false);
        } else if (index < machineSlots + 27) {
            // the machine first, then the other half of the player inventory: the ranges never overlap, so a full
            // machine still falls back into the hotbar exactly like an empty one
            moved = moveItemStackTo(stack, 0, machineSlots, false);
            moved |= moveItemStackTo(stack, machineSlots + 27, slots.size(), false);
        } else {
            moved = moveItemStackTo(stack, 0, machineSlots, false);
            moved |= moveItemStackTo(stack, machineSlots, machineSlots + 27, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer)
            blockEntity.getInnerNetwork().removeObserver(serverPlayer.getUUID());
    }

    public UUID getUuid() {
        return blockEntity.getNetworkUUID();
    }
}
