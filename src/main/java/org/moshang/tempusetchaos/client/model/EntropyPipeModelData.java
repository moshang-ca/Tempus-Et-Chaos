package org.moshang.tempusetchaos.client.model;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class EntropyPipeModelData {
    public static final ModelProperty<Integer> FACE_MODES = new ModelProperty<>();
    public static final ModelProperty<Integer> NEIGHBOR_INSETS = new ModelProperty<>();
    public static final ModelProperty<Boolean> HAS_GAS = new ModelProperty<>();

    private EntropyPipeModelData() { }

    public static ModelData of(int modeMask, int insetMask, boolean hasGas) {
        return ModelData.builder()
                .with(FACE_MODES, modeMask)
                .with(NEIGHBOR_INSETS, insetMask)
                .with(HAS_GAS, hasGas)
                .build();
    }

    public static int modeOf(int modeMask, int faceIndex) {
        return (modeMask >>> (faceIndex * 2)) & 0b11;
    }


    public static int insetOf(int insetMask, int faceIndex) {
        return (insetMask >>> (faceIndex * 4)) & 0xF;
    }
}
