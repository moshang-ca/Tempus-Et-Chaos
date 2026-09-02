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
import org.moshang.tempusetchaos.blockentity.BETimeExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockTimeExtractor extends Block implements EntityBlock {
    public BlockTimeExtractor(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BETimeExtractor node) {
                IChrononNode.onNodePlaced(level, pos, node);
                if (BETimeExtractor.PRODUCE_CONDITIONS.contains(level.getBlockState(pos.below()).getBlock()))
                    node.setCanProduce(true);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.getBlockEntity(neighborPos.above()) instanceof BETimeExtractor node) {
            node.setCanProduce(BETimeExtractor.PRODUCE_CONDITIONS.contains(level.getBlockState(neighborPos).getBlock()));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BETimeExtractor(pos, state);
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
