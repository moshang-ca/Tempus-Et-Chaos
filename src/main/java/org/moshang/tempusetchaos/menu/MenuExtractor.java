package org.moshang.tempusetchaos.menu;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.moshang.tempusetchaos.blockentity.BETimeExtractor;
import org.moshang.tempusetchaos.registry.TECBlocks;
import org.moshang.tempusetchaos.registry.TECMenus;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MenuExtractor extends ChrononMenu<BETimeExtractor>{

    public MenuExtractor(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (BETimeExtractor) inventory.player.level().getBlockEntity(pos));
    }

    public MenuExtractor(int containerId, Inventory inventory, BETimeExtractor blockEntity) {
        super(TECMenus.TIME_EXTRACTOR.get(), containerId, inventory, blockEntity);

        IItemHandler handler = blockEntity.getInputHandler();
        addSlot(new SlotItemHandler(handler, 0, 33, 33));
        addSlot(new SlotItemHandler(handler, 1, 51, 53));
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null
                && stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, TECBlocks.TIME_EXTRACTOR.get());
    }
}
