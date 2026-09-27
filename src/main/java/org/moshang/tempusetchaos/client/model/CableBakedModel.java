package org.moshang.tempusetchaos.client.model;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.moshang.tempusetchaos.block.BlockChrononNetCable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@MethodsReturnNonnullByDefault
public class CableBakedModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ModelState DEFAULT_MODEL_STATE = new ModelState() {};

    protected static final float[] UV_CORE = {0f, 0f, 8f, 8f};
    protected static final float[] UV_ARM_U = {8f, 0f, 16f, 8f};
    protected static final float[] UV_ARM_V = {0f, 8f, 8f, 16f};
    protected static final float[] UV_FLOW = {0f, 0f, 16f, 16f};

    private static final ChunkRenderTypeSet RENDER_TYPES =
            ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent());

    protected final TextureAtlasSprite sprite;
    @Nullable
    protected final TextureAtlasSprite sprite2;

    protected final float[] cornerVertices;
    protected final float centerMin;
    protected final float centerMax;
    protected final float flowMin;
    protected final float flowMax;

    public CableBakedModel(TextureAtlasSprite sprite, float[] cornerVertices, float centerMin, float centerMax) {
        this(sprite, null, cornerVertices, centerMin, centerMax);
    }

    public CableBakedModel(TextureAtlasSprite sprite, @Nullable TextureAtlasSprite sprite2, float[] cornerVertices, float centerMin, float centerMax) {
        this.sprite = sprite;
        this.sprite2 = sprite2;
        this.cornerVertices = cornerVertices;
        this.centerMin = centerMin;
        this.centerMax = centerMax;
        float inset = (centerMax - centerMin) / 8f;
        this.flowMin = centerMin + inset;
        this.flowMax = centerMax - inset;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random) {
        return getQuads(state, direction, random, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @NotNull RandomSource random,
                                    @NotNull ModelData modelData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        if (direction != null) return quads;
        boolean inner = renderType == RenderType.translucent();
        if (inner && !hasFlow(modelData)) return quads;
        if (inner) addBox(quads, sprite2, flowMin, flowMin, flowMin, flowMax, flowMax, flowMax, UV_FLOW);
        else addBox(quads, sprite, cornerVertices, UV_CORE);
        int mask = state == null ? 0 : state.getValue(BlockChrononNetCable.CONNECTIONS);
        emitArms(quads, mask, modelData, inner);
        return quads;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean hasFlow(ModelData modelData) {
        return sprite2 != null;
    }

    protected void emitArms(List<BakedQuad> quads, int mask, ModelData modelData, boolean inner) {
        for (Direction dir : Direction.values()) {
            if ((mask & (1 << dir.get3DDataValue())) == 0) continue;
            if (!inner) {
                addArm(quads, sprite, centerMin, centerMax, dir);
                continue;
            }
            float[] b = armBounds(dir, flowMin, flowMax, 0f);
            addBox(quads, sprite2, b[0], b[1], b[2], b[3], b[4], b[5],
                    face -> face == dir ? null : UV_FLOW);
        }
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite;
    }

    protected static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float x0, float y0, float z0,
                                 float x1, float y1, float z1, float[] uv) {
        addBox(quads, sprite, x0, y0, z0, x1, y1, z1, dir -> uv);
    }

    protected static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float x0, float y0, float z0,
                                 float x1, float y1, float z1, Function<Direction, float[]> uvOf) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : Direction.values()) {
            float[] uv = uvOf.apply(dir);
            if (uv == null) continue;
            BlockElementFace face = new BlockElementFace(dir, 0, "", new BlockFaceUV(uv.clone(), 0));
            BakedQuad quad = BAKERY.bakeQuad(
                    from, to, face, sprite, dir,
                    DEFAULT_MODEL_STATE, null, false
            );
            quads.add(quad);
        }
    }

    protected static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float[] vertices, float[] uv) {
        addBox(quads, sprite, vertices[0], vertices[1], vertices[2], vertices[3], vertices[4], vertices[5], uv);
    }

    protected static float[] armBounds(Direction dir, float centerMin, float centerMax, float inset) {
        return switch (dir) {
            case NORTH -> new float[]{centerMin, centerMin, inset,     centerMax,   centerMax, centerMin};
            case SOUTH -> new float[]{centerMin, centerMin, centerMax, centerMax,   centerMax, 16f - inset};
            case WEST  -> new float[]{inset,     centerMin, centerMin, centerMin,   centerMax, centerMax};
            case EAST  -> new float[]{centerMax, centerMin, centerMin, 16f - inset, centerMax, centerMax};
            case DOWN  -> new float[]{centerMin, inset,     centerMin, centerMax,   centerMin, centerMax};
            case UP    -> new float[]{centerMin, centerMax, centerMin, centerMax,   16f - inset, centerMax};
        };
    }

    protected static void addArm(List<BakedQuad> quads, TextureAtlasSprite sprite, float centerMin, float centerMax, Direction dir) {
        addArm(quads, sprite, centerMin, centerMax, dir, 0f);
    }

    protected static void addArm(List<BakedQuad> quads, TextureAtlasSprite sprite, float centerMin, float centerMax, Direction dir, float inset) {
        float[] b = armBounds(dir, centerMin, centerMax, inset);
        addBox(quads, sprite, b[0], b[1], b[2], b[3], b[4], b[5],
                face -> face == dir ? null : armFaceUv(face, dir));
    }

    protected static float[] armFaceUv(Direction face, Direction arm) {
        Direction.Axis axis = arm.getAxis();
        boolean uRunsAlong = switch (face) {
            case UP, DOWN -> axis == Direction.Axis.X;      // u -> x, v -> z
            case NORTH, SOUTH -> axis == Direction.Axis.X;  // u -> x, v -> y
            case EAST, WEST -> axis == Direction.Axis.Z;    // u -> z, v -> y
        };
        return uRunsAlong ? UV_ARM_U : UV_ARM_V;
    }
}
