package org.moshang.tempusetchaos.client.model;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class EntropyPipeModelData {
    public static final ModelProperty<Integer> FACE_MODES = new ModelProperty<>();

    private EntropyPipeModelData() { }

    public static ModelData of(int modeMask) {
        return ModelData.builder().with(FACE_MODES, modeMask).build();
    }

    public static int modeOf(int modeMask, int faceIndex) {
        return (modeMask >>> (faceIndex * 2)) & 0b11;
    }
}
