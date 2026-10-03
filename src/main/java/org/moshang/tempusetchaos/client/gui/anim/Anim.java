package org.moshang.tempusetchaos.client.gui.anim;

import net.minecraft.Util;

/**
 * Time base and interpolation helpers.
 * Stateless: animations read the clock through here instead of carrying their own timers, so they stay
 * independent of both the tick rate and the frame rate.
 */
public final class Anim {
    public static final float EPSILON = 0.001f;

    private Anim() {}

    public static long now() {
        return Util.getMillis();
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    /** Elapsed fraction of a duration, clamped to [0, 1]. */
    public static float normalize(long elapsedMs, long durationMs) {
        if (durationMs <= 0) return 1f;
        return clamp01(elapsedMs / (float) durationMs);
    }

    public static boolean finished(long elapsedMs, long durationMs) {
        return elapsedMs >= durationMs;
    }
}
