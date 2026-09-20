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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moshang.tempusetchaos.TempusEtChaos;
import org.moshang.tempusetchaos.api.EntropyPipeFaceMode;
import org.moshang.tempusetchaos.api.IWrench;
import org.moshang.tempusetchaos.api.IWrenchable;
import org.moshang.tempusetchaos.blockentity.BEEntropyPipe;
import org.moshang.tempusetchaos.blockentity.network.PipeNetManager;
import org.moshang.tempusetchaos.client.model.EntropyPipeBakedModel;
import org.moshang.tempusetchaos.registry.TECCapabilities;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.moshang.tempusetchaos.block.BlockChrononNetCable.CONNECTIONS;

@ParametersAreNonnullByDefault
public class BlockEntropyPipe extends Block implements EntityBlock, IWrenchable {
    private static final float CENTER_MIN = 5f;
    private static final float CENTER_MAX = 11f;
    private static final float TIP_LENGTH = 2f;
    private static final float ARM_INSET = 1f;
    private static final float TIP_EXPAND = 1f;

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    private static final Map<Integer, VoxelShape> MODE_SHAPES = new ConcurrentHashMap<>();

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE;
            for (Direction dir : Direction.values()) {
                // 连接位序由 BlockChrononNetCable 定义（1 << get3DDataValue()），这里跟着走，不另立一份顺序表
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    shape = Shapes.or(shape, armBox(dir, 0f));
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
    public boolean onInteraction(Level level, BlockState state, IWrench.WrenchHit hit) {
        BlockPos pos = hit.pos();
        if (level.getBlockEntity(pos) instanceof BEEntropyPipe pipe) {
            Direction face = nearestConnectedFace(level, pos, level.getBlockState(pos), hit.location());
            if (face == null) return false;
            pipe.cycleFaceMode(face);
            return true;
        }
        return false;
    }

    private static Direction nearestConnectedFace(Level level, BlockPos pos, BlockState state, Vec3 location) {
        double lx = location.x - pos.getX();
        double ly = location.y - pos.getY();
        double lz = location.z - pos.getZ();
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            if (!isConnected(state, dir)) continue;
            if (level.getBlockEntity(pos.relative(dir)) instanceof BEEntropyPipe) continue;
            double distance = switch (dir) {
                case DOWN -> ly;
                case UP -> 1.0D - ly;
                case NORTH -> lz;
                case SOUTH -> 1.0D - lz;
                case WEST -> lx;
                case EAST -> 1.0D - lx;
            };
            if (distance < bestDistance) {
                bestDistance = distance;
                best = dir;
            }
        }
        return best;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            if (isConnected(state, dir) != shouldPipeConnect(level, pos, dir)) {
                BlockPos delta = neighborPos.subtract(pos);
                updateConnections(level, pos, Direction.fromDelta(delta.getX(), delta.getY(), delta.getZ()));
                return;
            }
        }
    }

    @Override
    @NotNull
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int connections = state.getValue(CONNECTIONS) & 0x3F;
        int modes = level.getBlockEntity(pos) instanceof BEEntropyPipe pipe ? pipe.encodeFaceModes() : 0;
        if (modes == 0) return SHAPES[connections];

        int key = (connections << 12) | modes;
        VoxelShape cached = MODE_SHAPES.get(key);
        if (cached != null) return cached;

        VoxelShape built = buildShape(connections, modes);
        if (MODE_SHAPES.size() > 1024) MODE_SHAPES.clear();
        MODE_SHAPES.put(key, built);
        return built;
    }

    private static VoxelShape buildShape(int connections, int modes) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            // 连接位与 BlockChrononNetCable 同源，用 get3DDataValue() 取
            if ((connections & (1 << dir.get3DDataValue())) == 0) continue;
            // 面模式的两位按 Direction.ordinal() 排布，对应 BEEntropyPipe#encodeFaceModes
            int mode = (modes >>> (dir.ordinal() * 2)) & 0b11;
            if (mode == EntropyPipeFaceMode.EXTRACT.ordinal()) {
                shape = Shapes.or(shape, armBox(dir, ARM_INSET), tipBox(dir, true));
            } else if (mode == EntropyPipeFaceMode.OUTPUT.ordinal()) {
                shape = Shapes.or(shape, armBox(dir, ARM_INSET), tipBox(dir, false));
            } else {
                shape = Shapes.or(shape, armBox(dir, 0f));
            }
        }
        return shape;
    }

    private static VoxelShape armBox(Direction dir, float inset) {
        return switch (dir) {
            case NORTH -> Block.box(CENTER_MIN, CENTER_MIN, inset, CENTER_MAX, CENTER_MAX, CENTER_MIN);
            case SOUTH -> Block.box(CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX, 16 - inset);
            case WEST  -> Block.box(inset, CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX);
            case EAST  -> Block.box(CENTER_MAX, CENTER_MIN, CENTER_MIN, 16 - inset, CENTER_MAX, CENTER_MAX);
            case DOWN  -> Block.box(CENTER_MIN, inset, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX);
            case UP    -> Block.box(CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, 16 - inset, CENTER_MAX);
        };
    }

    private static VoxelShape tipBox(Direction dir, boolean flared) {
        float expand = flared ? TIP_EXPAND : -TIP_EXPAND;
        float lo = CENTER_MIN - expand;
        float hi = CENTER_MAX + expand;
        float t = TIP_LENGTH;
        return switch (dir) {
            case NORTH -> Block.box(lo, lo, 0, hi, hi, t);
            case SOUTH -> Block.box(lo, lo, 16 - t, hi, hi, 16);
            case WEST  -> Block.box(0, lo, lo, t, hi, hi);
            case EAST  -> Block.box(16 - t, lo, lo, 16, hi, hi);
            case DOWN  -> Block.box(lo, 0, lo, hi, t, hi);
            case UP    -> Block.box(lo, 16 - t, lo, hi, 16, hi);
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BEEntropyPipe(pos, state);
    }

    private void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BEEntropyPipe pipe = (BEEntropyPipe) level.getBlockEntity(pos);
        assert pipe != null;
        if (!(state.getBlock() instanceof BlockEntropyPipe)) return;

        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            boolean connect = shouldPipeConnect(level, pos, dir);
            if (!connect) pipe.setFaceMode(dir, EntropyPipeFaceMode.NONE);
            newState = BlockChrononNetCable.setConnection(newState, dir, connect);
        }
        if (!newState.equals(state)) {
            level.setBlock(pos, newState, 3);
        }
    }

    private void updateConnections(Level level, BlockPos pos, Direction dir) {
        BlockState state = level.getBlockState(pos);
        BEEntropyPipe pipe = (BEEntropyPipe) level.getBlockEntity(pos);
        assert pipe != null;
        if (!(state.getBlock() instanceof BlockEntropyPipe)) return;

        BlockState newState = state;
        boolean connect = shouldPipeConnect(level, pos, dir);
        if (!connect) pipe.setFaceMode(dir, EntropyPipeFaceMode.NONE);
        newState = BlockChrononNetCable.setConnection(newState, dir, connect);
        if (!newState.equals(state))
            level.setBlock(pos, newState, 3);
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return BlockChrononNetCable.isConnected(state, dir);
    }

    public static boolean shouldPipeConnect(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        BlockState neighborState = level.getBlockState(neighborPos);
        return level.getCapability(TECCapabilities.FLUID_ENTROPY, neighborPos, neighborState, neighbor, dir) != null;
//        if (neighbor instanceof IEntropyPipeConnectable connectable)
//            return connectable.canPipeConnect(dir.getOpposite());
//        if (neighbor instanceof IChrononNode)
//            return true;
//        return neighborState.getBlock() instanceof IEntropyPipeConnectable connectable
//                && connectable.canPipeConnect(dir.getOpposite());
    }

    @ParametersAreNonnullByDefault
    public final static class EntropyPipeGeometry implements IUnbakedGeometry<EntropyPipeGeometry> {
        @Override
        @NotNull
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(TempusEtChaos.MODID, "block/entropy_pipe")));
            return new EntropyPipeBakedModel(sprite, new float[]{ 5, 5, 5, 11, 11, 11 }, 5, 11);
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
