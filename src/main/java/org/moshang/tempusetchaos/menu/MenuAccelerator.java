package org.moshang.tempusetchaos.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECMenus;

public class MenuAccelerator extends ChrononMenu<BEAccelerator> {
    private final ContainerData data;

    // Client
    public MenuAccelerator(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (BEAccelerator) inventory.player.level().getBlockEntity(pos));
    }

    public MenuAccelerator(int containerId, Inventory inventory, BEAccelerator be) {
        super(TECMenus.ACCELERATOR.get(), containerId, inventory, be);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? blockEntity.getAccelerateMultiplier() : BEAccelerator.MIN_MULTIPLIER;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.addDataSlots(data);
    }


    @Override
    public boolean stillValid(@NotNull Player player) {
        return blockEntity.getLevel() != null
                && stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, TECBlocks.ACCELERATOR.get());
    }

    public int getMultiplier() {
        return data.get(0);
    }
}
