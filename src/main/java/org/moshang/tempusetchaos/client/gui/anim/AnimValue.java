package org.moshang.tempusetchaos.client.gui.anim;

import lombok.Getter;

/**
 * <b>A driver</b>: the tween state of a single scalar, moving from its previous value to a target.
 * {@link #update()} is meant to be called once per frame by whoever drives it (see {@link Anims}); the
 * value itself is then read through {@link #getValue()}.
 * <p>
 * Elements that only need to <i>read</i> an animated number keep one of these as a handle and hand it to
 * an {@code Anims.tween(...)}. They must not call {@code to()} / {@code update()} themselves.
 */
@SuppressWarnings("unused")
public class AnimValue {
    @Getter
    private float value;
    private float from;
    private float to;
    private long startMs;
    private long delayMs;
    private long durationMs = 1L;
    private Easing easing = Easing.EASE_OUT_QUAD;
    @Getter
    private boolean running;

    public AnimValue(float initial) {
        this.value = this.from = this.to = initial;
    }

    /**
     * Snap to the target, no animation.
     */
    public void snap(float v) {
        value = from = to = v;
        this.delayMs = 0L;
        this.running = false;
    }

    public void to(float target, long delayMs, long durationMs, Easing easing) {
        if (running && this.to == target && this.delayMs == delayMs
                && this.durationMs == durationMs && this.easing == easing) return;
        this.from = value;
        this.to = target;
        this.durationMs = durationMs;
        this.delayMs = Math.max(0L, delayMs);
        this.startMs = Anim.now() + this.delayMs;
        this.easing = easing;
        this.running = true;
    }

    public void to(float target, long durationMs, Easing easing) {
        to(target, 0L, durationMs, easing);
    }

    public void to(float target, long durationMs) {
        to(target, durationMs, this.easing);
    }

    public void update() {
        if (!running) return;
        long elapsed = Anim.now() - startMs;
        if (elapsed < 0) return;
        if (Anim.finished(elapsed, durationMs)) {
            value = to;
            running = false;
            return;
        }
        value = Anim.lerp(from, to, easing.ease(Anim.normalize(elapsed, durationMs)));
    }
}
