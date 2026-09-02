package org.moshang.tempusetchaos.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.moshang.tempusetchaos.api.ICableConnectable;
import org.moshang.tempusetchaos.data.ChrononNetwork;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class BlockChrononNetCable extends Block implements ICableConnectable {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

    private static final int BIT_NORTH = 1;
    private static final int BIT_SOUTH = 1 << 1;
    private static final int BIT_EAST  = 1 << 2;
    private static final int BIT_WEST  = 1 << 3;
    private static final int BIT_UP    = 1 << 4;
    private static final int BIT_DOWN  = 1 << 5;

    public BlockChrononNetCable(Properties properties) {
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
        onCablePlaced(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            boolean shouldUpdate = false;
            for (Direction dir : Direction.values()) {
                boolean isConnected = isConnected(level.getBlockState(pos), dir);
                boolean shouldConnect = shouldConnect(level, pos, dir);
                if (isConnected != shouldConnect) {
                    shouldUpdate = true;
                    break;
                }
            }
            if (shouldUpdate) {
                updateConnections(level, pos);
            }
        }
    }

    private void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockChrononNetCable)) return;

        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            boolean connect = shouldConnect(level, pos, dir);
            newState = setConnection(newState, dir, connect);
        }
        level.setBlock(pos, newState, 3);
    }

    public static void onCablePlaced(Level level, BlockPos pos) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        Set<UUID> adjacent = ChrononNetwork.findAdjacent(serverLevel, pos);
        if (adjacent.size() > 1) {
            ChrononNetwork primary = null;
            for (UUID id : adjacent) {
                ChrononNetwork network = ChrononNetwork.NETWORK.get(id);
                if (network == null) continue;
                if (primary == null) {
                    primary = network;
                } else {
                    primary.merge(id);
                    System.out.println("merge network as cable connect");
                }
            }
        }
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        int mask = state.getValue(CONNECTIONS);
        return switch (dir) {
            case NORTH -> (mask & BIT_NORTH) != 0;
            case SOUTH -> (mask & BIT_SOUTH) != 0;
            case EAST  -> (mask & BIT_EAST) != 0;
            case WEST  -> (mask & BIT_WEST) != 0;
            case UP    -> (mask & BIT_UP) != 0;
            case DOWN  -> (mask & BIT_DOWN) != 0;
        };
    }

    public static boolean shouldConnect(Level level, BlockPos pos, Direction dir) {
        if (level.getBlockEntity(pos.relative(dir)) instanceof ICableConnectable connectable)
            return connectable.canConnect(dir);
        if (level.getBlockState(pos.relative(dir)).getBlock() instanceof ICableConnectable connectable)
            return connectable.canConnect(dir);
        else return false;
    }

    public static BlockState setConnection(BlockState state, Direction dir, boolean connect) {
        int mask = state.getValue(CONNECTIONS);
        int bit = switch (dir) {
            case NORTH -> BIT_NORTH;
            case SOUTH -> BIT_SOUTH;
            case EAST  -> BIT_EAST;
            case WEST  -> BIT_WEST;
            case UP    -> BIT_UP;
            case DOWN  -> BIT_DOWN;
        };
        if (connect) {
            mask |= bit;
        } else {
            mask &= ~bit;
        }
        return state.setValue(CONNECTIONS, mask);
    }
}
