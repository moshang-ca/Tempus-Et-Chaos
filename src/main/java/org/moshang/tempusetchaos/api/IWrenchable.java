package org.moshang.tempusetchaos.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IWrenchable {
    default boolean onWrench(Level level, IWrench.WrenchHit hit, boolean isShifted, @Nullable Player player) {
        BlockPos pos = hit.pos();
        BlockState state = level.getBlockState(pos);
        if (isShifted) return onShiftedInteraction(level, state, hit , player);
        else return onInteraction(level, state, hit);
    }

    default boolean onShiftedInteraction(Level level, BlockState state, IWrench.WrenchHit hitResult, @Nullable Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        var drops = Block.getDrops(state, serverLevel, hitResult.pos(), null);
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, hitResult.pos(), state, null, null);
        if (handler != null) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }
        level.removeBlock(hitResult.pos(), false);
        for (ItemStack drop : drops) {
            if (player == null || !player.getInventory().add(drop))
                Containers.dropItemStack(level, hitResult.pos().getX(), hitResult.pos().getY(), hitResult.pos().getZ(), drop);
        }
        return true;
    }

    default boolean onInteraction(Level level, BlockState state, IWrench.WrenchHit hit) {
        BlockState rotated = state.rotate(level, hit.pos(), Rotation.CLOCKWISE_90);
        if (rotated == state) return false;
        level.setBlockAndUpdate(hit.pos(), rotated);
        return true;
    }
}
