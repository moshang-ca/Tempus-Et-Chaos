package org.moshang.tempusetchaos.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.moshang.tempusetchaos.api.EntropyPipeFaceMode;

import java.util.List;

public class EntropyPipeBakedModel extends CableBakedModel {
    private static final float TIP_LENGTH = 2.0f;
    private static final float ARM_INSET = 1.0f;
    private static final float TIP_EXPAND = 1.0f;

    public EntropyPipeBakedModel(TextureAtlasSprite sprite, float[] cornerVertices, float centerMin, float centerMax) {
        super(sprite, cornerVertices, centerMin, centerMax);
    }

    @Override
    protected void emitArms(List<BakedQuad> quads, int mask, ModelData modelData) {
        Integer packed = modelData.get(EntropyPipeModelData.FACE_MODES);
        int modes = packed == null ? 0 : packed;
        for (Direction dir : Direction.values()) {
            if ((mask & (1 << dir.get3DDataValue())) == 0) continue;

            int mode = EntropyPipeModelData.modeOf(modes, dir.ordinal());
            if (mode == EntropyPipeFaceMode.EXTRACT.ordinal() || mode == EntropyPipeFaceMode.OUTPUT.ordinal()) {
                addArm(quads, sprite, centerMin, centerMax, dir, ARM_INSET);
                addTip(quads, dir, mode == EntropyPipeFaceMode.EXTRACT.ordinal());
            } else {
                addArm(quads, sprite, centerMin, centerMax, dir);
            }
        }
    }

    private void addTip(List<BakedQuad> quads, Direction dir, boolean flared) {
        float expand = flared ? TIP_EXPAND : -TIP_EXPAND;
        float lo = Math.max(0f, centerMin - expand);
        float hi = Math.min(16f, centerMax + expand);
        float t = TIP_LENGTH;

        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case NORTH -> { x0 = lo;      y0 = lo;      z0 = 0f;       x1 = hi;  y1 = hi;  z1 = t;     }
            case SOUTH -> { x0 = lo;      y0 = lo;      z0 = 16f - t;  x1 = hi;  y1 = hi;  z1 = 16f;   }
            case WEST  -> { x0 = 0f;      y0 = lo;      z0 = lo;       x1 = t;   y1 = hi;  z1 = hi;    }
            case EAST  -> { x0 = 16f - t; y0 = lo;      z0 = lo;       x1 = 16f; y1 = hi;  z1 = hi;    }
            case DOWN  -> { x0 = lo;      y0 = 0f;      z0 = lo;       x1 = hi;  y1 = t;   z1 = hi;    }
            case UP    -> { x0 = lo;      y0 = 16f - t; z0 = lo;       x1 = hi;  y1 = 16f; z1 = hi;    }
            default    -> { return; }
        }
        addBox(quads, sprite, x0, y0, z0, x1, y1, z1);
    }
}
