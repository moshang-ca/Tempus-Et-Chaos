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
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IWrenchable {
    default boolean onWrench(Level level, IWrench.WrenchHit hit, boolean isShifted, @Nullable Player player) {
        BlockPos pos = hit.pos();
        BlockState state = level.getBlockState(pos);
        if (isShifted) return onShiftedInteraction(level, pos, state, player);
        else return onInteraction(level, state, hit);
    }

    default boolean onShiftedInteraction(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        var drops = Block.getDrops(state, serverLevel, pos, null);
        level.removeBlock(pos, false);
        for (ItemStack drop : drops) {
            if (player == null || !player.getInventory().add(drop))
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), drop);
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
