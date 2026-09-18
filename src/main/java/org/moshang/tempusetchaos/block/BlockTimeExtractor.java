package org.moshang.tempusetchaos.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.BaseChrononMachineryBlock;
import org.moshang.tempusetchaos.api.BaseChrononNodeBlockEntity;
import org.moshang.tempusetchaos.blockentity.BETimeExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockTimeExtractor extends BaseChrononMachineryBlock {
    private static final MapCodec<BlockTimeExtractor> CODEC = simpleCodec(BlockTimeExtractor::new);

    public BlockTimeExtractor(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.getBlockEntity(neighborPos.above()) instanceof BETimeExtractor node) {
            node.setCanProduce(BETimeExtractor.PRODUCE_CONDITIONS.contains(level.getBlockState(neighborPos).getBlock()));
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (BETimeExtractor.PRODUCE_CONDITIONS.contains(level.getBlockState(pos.below()).getBlock())) {
            if (level.getBlockEntity(pos) instanceof BETimeExtractor node) {
                node.setCanProduce(true);
            }
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
