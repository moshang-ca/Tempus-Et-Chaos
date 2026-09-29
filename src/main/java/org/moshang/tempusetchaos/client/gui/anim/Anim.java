package org.moshang.tempusetchaos.client.gui.anim;

import net.minecraft.Util;

public final class Anim {
    private Anim() {}

    public static long now() {
        return Util.getMillis();
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
