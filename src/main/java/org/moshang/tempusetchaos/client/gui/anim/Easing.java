package org.moshang.tempusetchaos.client.gui.anim;

/**
 * Shape of a transition: maps a normalized progress to a normalized position. Most curves map
 * [0, 1] onto [0, 1]; back, elastic and bounce overshoot or undershoot on purpose.
 */
@SuppressWarnings({"unused"})
@FunctionalInterface
public interface Easing {
    Easing LINEAR = t -> t;

    // ---- Quad ----
    Easing EASE_IN_QUAD  = t -> t * t;
    Easing EASE_OUT_QUAD = t -> 1f - (1f - t) * (1f - t);
    Easing EASE_IN_OUT_QUAD = t -> t < 0.5f
            ? 2f * t * t
            : 1f - 2f * (1f - t) * (1f - t) / 2f;

    // ---- Cubic ----
    Easing EASE_IN_CUBIC  = t -> t * t * t;
    Easing EASE_OUT_CUBIC = t -> 1f - (float) Math.pow(1f - t, 3f);
    Easing EASE_IN_OUT_CUBIC = t -> t < 0.5f
            ? 4f * t * t * t
            : 1f - 4f * (1f - t) * (1f -t) * (1f - t);

    // ---- Quart ----
    Easing EASE_IN_QUART  = t -> t * t * t * t;
    Easing EASE_OUT_QUART = t -> 1f - (float) Math.pow(1f - t, 4f);
    Easing EASE_IN_OUT_QUART = t -> t < 0.5f
            ? 8f * t * t * t * t
            : 1f - 8f * (1f - t) * (1f - t) * (1f - t) * (1f - t);

    // ---- Sine ----
    Easing EASE_IN_SINE  = t -> 1f - (float) Math.cos(t * Math.PI * 0.5f);
    Easing EASE_OUT_SINE = t -> (float) Math.sin(t * Math.PI * 0.5f);
    Easing EASE_IN_OUT_SINE = t -> -(float) (Math.cos(Math.PI * t) - 1f) / 2f;

    // ---- Expo ----
    Easing EASE_IN_EXPO  = t -> t == 0f ? 0f : (float) Math.pow(2f, 10f * t - 10f);
    Easing EASE_OUT_EXPO = t -> t == 1f ? 1f : 1f - (float) Math.pow(2f, -10f * t);
    Easing EASE_IN_OUT_EXPO = t -> {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        return t < 0.5f
                ? (float) Math.pow(2f, 20f * t - 10f) / 2f
                : (2f - (float) Math.pow(2f, -20f * t + 10f)) / 2f;
    };

    // ---- Back----
    Easing EASE_IN_BACK  = t -> {
        float c = 1.70158f;
        return (c + 1f) * t * t * t - c * t * t;
    };
    Easing EASE_OUT_BACK = t -> {
        float c = 1.70158f;
        float u = t - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    };
    Easing EASE_IN_OUT_BACK = t -> {
        float c2 = 1.70158f * 1.525f;
        if (t < 0.5f) {
            float u = 2f * t;
            return (u * u * ((c2 + 1f) * u - c2)) / 2f;
        } else {
            float u = 2f * t - 2f;
            return (u * u * ((c2 + 1f) * u + c2) + 2f) / 2f;
        }
    };

    // ---- Elastic ----
    Easing EASE_IN_ELASTIC = t -> {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        float c4 = (2f * (float) Math.PI) / 3f;
        return -(float) Math.pow(2f, 10f * t - 10f) * (float) Math.sin((t * 10f - 10.75f) * c4);
    };
    Easing EASE_OUT_ELASTIC = t -> {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        float c4 = (2f * (float) Math.PI) / 3f;
        return (float) Math.pow(2f, -10f * t) * (float) Math.sin((t * 10f - 0.75f) * c4) + 1f;
    };
    Easing EASE_IN_OUT_ELASTIC = t -> {
        if (t == 0f) return 0f;
        if (t == 1f) return 1f;
        float c5 = (2f * (float) Math.PI) / 4.5f;
        return t < 0.5f
                ? -((float) Math.pow(2f, 20f * t - 10f) * (float) Math.sin((20f * t - 11.125f) * c5)) / 2f
                : ((float) Math.pow(2f, -20f * t + 10f) * (float) Math.sin((20f * t - 11.125f) * c5)) / 2f + 1f;
    };

    // ---- Bounce ----
    Easing EASE_IN_BOUNCE  = t -> 1f - outBounce(1f - t);
    Easing EASE_OUT_BOUNCE = Easing::outBounce;
    Easing EASE_IN_OUT_BOUNCE = t -> t < 0.5f
            ? (1f - outBounce(1f - 2f * t)) / 2f
            : (1f + outBounce(2f * t - 1f)) / 2f;

    static float outBounce(float t) {
        float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1f / d1) return n1 * t * t;
        if (t < 2f / d1) {
            float u = t - 1.5f / d1;
            return n1 * u * u + 0.75f;
        }
        if (t < 2.5f / d1) {
            float u = t - 2.25f / d1;
            return n1 * u * u + 0.9375f;
        }
        float u = t - 2.625f / d1;
        return n1 * u * u + 0.984375f;
    }

    float ease(float t);

    static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        return t -> {
            float lo = 0f, hi = 1f;
            for (int i = 0; i < 20; i++) {
                float mid = (lo + hi) * 0.5f;
                float x = bezier(mid, x1, x2);
                if (x < t) lo = mid; else hi = mid;
            }
            float guess = (lo + hi) * 0.5f;
            return bezier(guess, y1, y2);
        };
    }

    private static float bezier(float t, float p1, float p2) {
        float u = 1f - t;
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t;
    }
}
