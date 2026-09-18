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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.IChrononNode;
import org.moshang.tempusetchaos.api.IEntropyPipeConnectable;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;
import org.moshang.tempusetchaos.blockentity.network.PipeNetManager;
import org.moshang.tempusetchaos.client.model.CableBakedModel;
import org.moshang.tempusetchaos.registry.TECCapabilities;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.function.Function;

import static org.moshang.tempusetchaos.block.BlockChrononNetCable.CONNECTIONS;

@ParametersAreNonnullByDefault
public class BlockEntropyPipe extends Block implements IEntropyPipeConnectable, EntityBlock {
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
        BlockState neighborState = level.getBlockState(neighborPos);
        if (level.getCapability(TECCapabilities.FLUID_ENTROPY, neighborPos, neighborState, neighbor, dir) == null)
            return false;
        if (neighbor instanceof IEntropyPipeConnectable connectable)
            return connectable.canPipeConnect(dir.getOpposite());
        if (neighbor instanceof IChrononNode)
            return true;
        return neighborState.getBlock() instanceof IEntropyPipeConnectable connectable
                && connectable.canPipeConnect(dir.getOpposite());
    }

    @ParametersAreNonnullByDefault
    public final static class EntropyPipeGeometry implements IUnbakedGeometry<EntropyPipeGeometry> {
        @Override
        @NotNull
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "block/entropy_pipe")));
            return new CableBakedModel(sprite, new float[]{ 4, 4, 4, 12, 12, 12 }, 4, 12);
        }
    }

    @ParametersAreNonnullByDefault
    public final static class EntropyPipeGeometryLoader implements IGeometryLoader<EntropyPipeGeometry> {
        @Override
        @NotNull
        public EntropyPipeGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
            return new EntropyPipeGeometry();
        }
    }
}
