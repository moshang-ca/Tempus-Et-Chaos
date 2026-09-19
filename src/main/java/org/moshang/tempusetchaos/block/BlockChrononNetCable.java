package org.moshang.tempusetchaos.block;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.ICableConnectable;
import org.moshang.tempusetchaos.client.model.CableBakedModel;
import org.moshang.tempusetchaos.blockentity.network.ChrononNetwork;
import org.moshang.tempusetchaos.data.ChrononNetworkData;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public class BlockChrononNetCable extends Block implements ICableConnectable {
    public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape[] tmp = {
                Block.box(6, 0, 6, 10, 6, 10),
                Block.box(6, 10, 6, 10, 16, 10),
                Block.box(6, 6, 0, 10, 10, 6),
                Block.box(6, 6, 10, 10, 10, 16),
                Block.box(0, 6, 6, 6, 10, 10),
                Block.box(10, 6, 6, 16, 10, 10)
        };
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, tmp[i]);
                }
            }
            SHAPES[mask] = shape;
        }
    }


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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock()))
            onCableRemoved(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
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

    @Override
    @NotNull
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = state.getValue(CONNECTIONS);
        return SHAPES[mask & 0x3F];
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
        if (!(level instanceof ServerLevel serverLevel)) return;

        Set<UUID> adjacent = ChrononNetwork.findAdjacent(serverLevel, pos);
        if (adjacent.size() > 1) {
            ChrononNetwork primary = null;
            for (UUID id : adjacent) {
                ChrononNetwork network = ChrononNetworkData.getLevelNetwork(level, id);
                if (network == null) continue;
                if (primary == null) {
                    primary = network;
                } else {
                    primary.merge(id);
                }
            }
        }
    }

    public static void onCableRemoved(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        ChrononNetwork.checkAndSplit(level, pos);
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        int mask = state.getValue(CONNECTIONS);
        return (mask & (1 << dir.get3DDataValue())) != 0;
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
        if (connect) {
            mask |= (1 << dir.get3DDataValue());
        } else {
            mask &= ~(1 << dir.get3DDataValue());
        }
        return state.setValue(CONNECTIONS, mask);
    }

    @ParametersAreNonnullByDefault
    public final static class ChrononCableGeometry implements IUnbakedGeometry<ChrononCableGeometry> {
        @Override
        @NotNull
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "block/chronon_cable")));
            return new CableBakedModel(sprite, new float[]{ 6, 6, 6, 10, 10, 10 }, 6, 10);
        }
    }

    @ParametersAreNonnullByDefault
    public final static class ChrononCableGeometryLoader implements IGeometryLoader<ChrononCableGeometry> {
        @Override
        @NotNull
        public ChrononCableGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
            return new ChrononCableGeometry();
        }
    }
}
