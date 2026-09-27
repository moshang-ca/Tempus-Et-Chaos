package org.moshang.tempusetchaos.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.moshang.tempusetchaos.api.EntropyPipeFaceMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class EntropyPipeBakedModel extends CableBakedModel {
    private static final float TIP_LENGTH = 2.0f;
    private static final float ARM_INSET = 1.0f;
    private static final float TIP_EXPAND = 1.0f;
    private static final int CACHE_LIMIT = 512;

    private static final float[] UV_TIP = {8f, 8f, 16f, 16f};

    private final Map<Long, List<BakedQuad>> armsCache = new ConcurrentHashMap<>();

    public EntropyPipeBakedModel(TextureAtlasSprite sprite, TextureAtlasSprite sprite2, float[] cornerVertices, float centerMin, float centerMax) {
        super(sprite, sprite2, cornerVertices, centerMin, centerMax);
    }

    @Override
    protected boolean hasFlow(ModelData modelData) {
        return super.hasFlow(modelData) && Boolean.TRUE.equals(modelData.get(EntropyPipeModelData.HAS_GAS));
    }

    @Override
    protected void emitArms(List<BakedQuad> quads, int mask, ModelData modelData, boolean inner) {
        Integer packedModes = modelData.get(EntropyPipeModelData.FACE_MODES);
        int modes = packedModes == null ? 0 : packedModes;
        Integer packedInsets = modelData.get(EntropyPipeModelData.NEIGHBOR_INSETS);
        int insets = packedInsets == null ? 0 : packedInsets;

        long key = ((long) mask << 40) | ((long) modes << 26) | ((long) insets << 1) | (inner ? 1L : 0L);
        List<BakedQuad> arms = armsCache.get(key);
        if (arms == null) {
            List<BakedQuad> built = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) == 0) continue;

                int extend = EntropyPipeModelData.insetOf(insets, dir.ordinal());
                int mode = EntropyPipeModelData.modeOf(modes, dir.ordinal());
                boolean port = mode == EntropyPipeFaceMode.EXTRACT.ordinal() || mode == EntropyPipeFaceMode.OUTPUT.ordinal();
                float inset = port ? ARM_INSET : -extend;

                if (inner) {
                    float[] b = armBounds(dir, flowMin, flowMax, inset);
                    addBox(built, sprite2, b[0], b[1], b[2], b[3], b[4], b[5],
                            face -> face == dir ? null : UV_FLOW);
                } else {
                    addArm(built, sprite, centerMin, centerMax, dir, inset);
                }
                if (port) addTip(built, dir, mode == EntropyPipeFaceMode.EXTRACT.ordinal(), extend, inner);
            }
            arms = List.copyOf(built);
            if (armsCache.size() >= CACHE_LIMIT) armsCache.clear();
            armsCache.put(key, arms);
        }
        quads.addAll(arms);
    }

    private void addTip(List<BakedQuad> quads, Direction dir, boolean flared, int extend, boolean inner) {
        float expand = flared ? TIP_EXPAND : -TIP_EXPAND;
        float lo = Math.max(0f, centerMin - expand);
        float hi = Math.min(16f, centerMax + expand);
        if (inner) {
            float shrink = (hi - lo) / 8f;
            lo += shrink;
            hi -= shrink;
        }
        float t = TIP_LENGTH;
        float e = extend;

        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case NORTH -> { x0 = lo;      y0 = lo;      z0 = -e;       x1 = hi;      y1 = hi;      z1 = t;          }
            case SOUTH -> { x0 = lo;      y0 = lo;      z0 = 16f - t;  x1 = hi;      y1 = hi;      z1 = 16f + e;    }
            case WEST  -> { x0 = -e;      y0 = lo;      z0 = lo;       x1 = t;       y1 = hi;      z1 = hi;         }
            case EAST  -> { x0 = 16f - t; y0 = lo;      z0 = lo;       x1 = 16f + e; y1 = hi;      z1 = hi;         }
            case DOWN  -> { x0 = lo;      y0 = -e;      z0 = lo;       x1 = hi;      y1 = t;       z1 = hi;         }
            case UP    -> { x0 = lo;      y0 = 16f - t; z0 = lo;       x1 = hi;      y1 = 16f + e; z1 = hi;         }
            default    -> { return; }
        }
        Function<Direction, float[]> uvOf = inner
                ? face -> face == dir || face == dir.getOpposite() ? null : UV_FLOW
                : face -> face == dir ? UV_TIP : face == dir.getOpposite() ? null : armFaceUv(face, dir);
        addBox(quads, inner ? sprite2 : sprite, x0, y0, z0, x1, y1, z1, uvOf);
    }
}
