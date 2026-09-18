package org.moshang.tempusetchaos.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.api.IChrononNode;
import org.moshang.tempusetchaos.api.IEntropyPipeConnectable;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;
import org.moshang.tempusetchaos.blockentity.network.PipeNetManager;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BlockEntropyPipe extends Block implements IEntropyPipeConnectable, EntityBlock {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape core = Block.box(6, 6, 6, 10, 10, 10);
        VoxelShape[] arms = {
                Block.box(6, 6, 0, 10, 10, 6),
                Block.box(6, 6, 10, 10, 10, 16),
                Block.box(10, 6, 6, 16, 10, 10),
                Block.box(0, 6, 6, 6, 10, 10),
                Block.box(6, 10, 6, 10, 16, 10),
                Block.box(6, 0, 6, 10, 6, 10)
        };
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, arms[i]);
                }
            }
            SHAPES[mask] = shape;
        }
    }

    public BlockEntropyPipe(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(CONNECTIONS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) || !oldState.equals(state)) {
            updateConnections(level, pos);
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
            pipe.setChanged();
            PipeNetManager.enqueue(pipe);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock()) && level instanceof ServerLevel serverLevel) {
            PipeNetManager.get(serverLevel).onPipeBroken(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            if (isConnected(state, dir) != shouldPipeConnect(level, pos, dir)) {
                updateConnections(level, pos);
                return;
            }
        }
    }

    @Override
    @NotNull
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(CONNECTIONS) & 0x3F];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BEEntropyPipe(pos, state);
    }

    private void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockEntropyPipe)) return;

        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            newState = BlockChrononNetCable.setConnection(newState, dir, shouldPipeConnect(level, pos, dir));
        }
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, 3);
        }
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return BlockChrononNetCable.isConnected(state, dir);
    }

    public static boolean shouldPipeConnect(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof IEntropyPipeConnectable connectable) {
            return connectable.canPipeConnect(dir.getOpposite());
        }
        if (neighbor instanceof IChrononNode) {
            return true;
        }
        return level.getBlockState(neighborPos).getBlock() instanceof IEntropyPipeConnectable connectable
                && connectable.canPipeConnect(dir.getOpposite());
    }

}
