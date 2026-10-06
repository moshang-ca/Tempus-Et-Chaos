package org.moshang.tempusetchaos.menu;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECMenus;

public class MenuAccelerator extends AbstractContainerMenu {
    @Getter
    private final BEAccelerator beAccelerator;
    private final ContainerData data;

    // Client
    public MenuAccelerator(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (BEAccelerator) inventory.player.level().getBlockEntity(pos));
    }

    public MenuAccelerator(int containerId, Inventory inventory, BEAccelerator be) {
        super(TECMenus.ACCELERATOR.get(), containerId);
        beAccelerator = be;

        for (int l = 0; l < 3; l++) {
            for (int j1 = 0; j1 < 9; j1++) {
                this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142));
        }

        this.data = new SimpleContainerData(1);
        this.addDataSlots(data);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 27) {
            if (!this.moveItemStackTo(stack, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 36) {
            if (!this.moveItemStackTo(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return beAccelerator.getLevel() != null && stillValid(ContainerLevelAccess.create(beAccelerator.getLevel(), beAccelerator.getBlockPos()),
                player, TECBlocks.ACCELERATOR.get());
    }

    public int getMultiplier() {
        return data.get(0);
    }
}
