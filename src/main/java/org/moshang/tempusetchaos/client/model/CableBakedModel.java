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
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.moshang.tempusetchaos.block.BlockChrononNetCable;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
public class CableBakedModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ModelState DEFAULT_MODEL_STATE = new ModelState() {};

    protected final TextureAtlasSprite sprite;

    protected final float[] cornerVertices;
    protected final float centerMin;
    protected final float centerMax;

    public CableBakedModel(TextureAtlasSprite sprite, float[] cornerVertices, float centerMin, float centerMax) {
        this.sprite = sprite;
        this.cornerVertices = cornerVertices;
        this.centerMin = centerMin;
        this.centerMax = centerMax;
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
        if (state == null) return quads;

        addBox(quads, sprite, cornerVertices);
        emitArms(quads, state.getValue(BlockChrononNetCable.CONNECTIONS), modelData);
        return quads;
    }

    protected void emitArms(List<BakedQuad> quads, int mask, ModelData modelData) {
        for (Direction dir : Direction.values()) {
            if ((mask & (1 << dir.get3DDataValue())) != 0) addArm(quads, sprite, centerMin, centerMax, dir);
        }
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
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
                                 float x1, float y1, float z1) {
        Vector3f from = new Vector3f(x0, y0, z0);
        Vector3f to = new Vector3f(x1, y1, z1);
        for (Direction dir : Direction.values()) {
            BlockElementFace face = new BlockElementFace(dir, 0, "", new BlockFaceUV(new float[]{0, 0, 16, 16}, 0));
            BakedQuad quad = BAKERY.bakeQuad(
                    from, to, face, sprite, dir,
                    DEFAULT_MODEL_STATE, null, false
            );
            quads.add(quad);
        }
    }

    protected static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float[] vertices) {
        addBox(quads, sprite, vertices[0], vertices[1], vertices[2], vertices[3], vertices[4], vertices[5]);
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
        addBox(quads, sprite, b[0], b[1], b[2], b[3], b[4], b[5]);
    }
}
