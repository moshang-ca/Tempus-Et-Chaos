package org.moshang.tempusetchaos.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.api.IChrononNode;
import org.moshang.tempusetchaos.blockentity.BEAccelerator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockAccelerator extends Block implements EntityBlock {
    public BlockAccelerator(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IChrononNode node) {
                IChrononNode.onNodePlaced(level, pos, node);
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BEAccelerator(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null :(lvl, pos, st, be) -> {
            if (be instanceof BaseChrononNodeBlockEntity blockEntity) {
                blockEntity.serverTick();
            }
        };
    }
}
