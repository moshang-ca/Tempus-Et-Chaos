package org.moshang.tempusetchaos.client.model;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int BIT_NORTH = 1;
    private static final int BIT_SOUTH = 1 << 1;
    private static final int BIT_EAST  = 1 << 2;
    private static final int BIT_WEST  = 1 << 3;
    private static final int BIT_UP    = 1 << 4;
    private static final int BIT_DOWN  = 1 << 5;

    private final TextureAtlasSprite sprite;

    private final float[] cornerVertices;
    private final float centerMin;
    private final float centerMax;

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
        List<BakedQuad> quads = new ArrayList<>();
        if (direction != null) return quads;
        if (state == null) return quads;

        int mask = state.getValue(BlockChrononNetCable.CONNECTIONS);
        addBox(quads, sprite, cornerVertices);

        if ((mask & BIT_NORTH) != 0) addArm(quads, sprite, centerMin, centerMax, Direction.NORTH);
        if ((mask & BIT_SOUTH) != 0) addArm(quads, sprite, centerMin, centerMax, Direction.SOUTH);
        if ((mask & BIT_EAST)  != 0) addArm(quads, sprite, centerMin, centerMax, Direction.EAST);
        if ((mask & BIT_WEST)  != 0) addArm(quads, sprite, centerMin, centerMax, Direction.WEST);
        if ((mask & BIT_UP)    != 0) addArm(quads, sprite, centerMin, centerMax, Direction.UP);
        if ((mask & BIT_DOWN)  != 0) addArm(quads, sprite, centerMin, centerMax, Direction.DOWN);
        return quads;
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

    private static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float x0, float y0, float z0,
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

    private static void addBox(List<BakedQuad> quads, TextureAtlasSprite sprite, float[] vertices) {
        addBox(quads, sprite, vertices[0], vertices[1], vertices[2], vertices[3], vertices[4], vertices[5]);
    }

    private static void addArm(List<BakedQuad> quads, TextureAtlasSprite sprite, float centerMin, float centerMax, Direction dir) {
        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case NORTH -> { x0 = centerMin; y0 = centerMin; z0 = 0f;        x1 = centerMax; y1 = centerMax; z1 = centerMin; }
            case SOUTH -> { x0 = centerMin; y0 = centerMin; z0 = centerMax; x1 = centerMax; y1 = centerMax; z1 = 16f;    }
            case EAST  -> { x0 = centerMax; y0 = centerMin; z0 = centerMin; x1 = 16f;       y1 = centerMax; z1 = centerMax; }
            case WEST  -> { x0 = 0f;        y0 = centerMin; z0 = centerMin; x1 = centerMin; y1 = centerMax; z1 = centerMax; }
            case UP    -> { x0 = centerMin; y0 = centerMax; z0 = centerMin; x1 = centerMax; y1 = 16f;       z1 = centerMax; }
            case DOWN  -> { x0 = centerMin; y0 = 0f;        z0 = centerMin; x1 = centerMax; y1 = centerMin; z1 = centerMax; }
            default    -> throw new IllegalStateException("Unexpected value: " + dir);
        }
        addBox(quads, sprite, x0, y0, z0, x1, y1, z1);
    }
}
