package org.moshang.tempusetchaos.client.gui.anim;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>A driver</b>: a keyframed timeline sampled over a duration, optionally waiting first, looping or ping-ponging.
 * Where {@link AnimValue} tweens a single segment, a track interpolates through many.
 * {@link #update()} is meant to be called once per frame by whoever drives it (see {@link Anims}); the
 * sampled number is read through {@link #getCurrent()}.
 */
@SuppressWarnings("unused")
public class Track {
    private final List<Keyframe> keys = new ArrayList<>();
    private long startMs;
    private long delayMs;
    private long durationMs = 1;
    private boolean loop;
    private boolean alternate;
    @Getter
    private boolean running;
    @Getter
    private float current;

    public Track key(float time, float value) {
        return key(time, value, Easing.EASE_IN_OUT_QUAD);
    }

    /**
     * @param time the normalized position on the timeline.
     * @param value the sample at that point.
     * */
    public Track key(float time, float value, Easing easing) {
        keys.add(new Keyframe(Math.clamp(time, 0f, 1f), value, easing));
        keys.sort((a, b) -> Float.compare(a.time, b.time));
        return this;
    }

    public void play(long durationMs, boolean loop, boolean alternate) {
        play(0L, durationMs, loop, alternate);
    }

    /**
     * @param delayMs how long the track holds its first key, measured from now.
     * @param durationMs how long the move itself takes, after that delay.
     */
    public void play(long delayMs, long durationMs, boolean loop, boolean alternate) {
        this.delayMs = Math.max(0L, delayMs);
        this.durationMs = Math.max(1, durationMs);
        this.startMs = Anim.now() + this.delayMs;
        this.loop = loop;
        this.alternate = alternate;
        this.running = true;
        update();
    }

    public void restart() {
        this.startMs = Anim.now() + delayMs;
        this.running = true;
        update();
    }

    public void stop() { running = false; }

    /** The value the track ends on, or its current one when it has no keys. */
    public float endValue() {
        return keys.isEmpty() ? current : keys.getLast().value();
    }

    public void snap(float v) {
        this.current = v;
        this.running = false;
    }

    public void update() {
        if (!running) return;
        // while the delay runs this stays 0, which samples the first key: the track waits without going idle
        long elapsed = Math.max(0L, Anim.now() - startMs);

        if (loop) {
            long total = alternate ? durationMs * 2 : durationMs;
            elapsed %= total;
            if (alternate && elapsed >= durationMs) {
                elapsed = total - elapsed;
            }
        } else if (Anim.finished(elapsed, durationMs)) {
            elapsed = durationMs;
            running = false;
        }

        current = sample(Anim.normalize(elapsed, durationMs));
    }

    private float sample(float t) {
        if (keys.isEmpty()) return current;
        if (keys.size() == 1) return keys.getFirst().value;

        Keyframe prev = keys.getFirst();
        if (t <= prev.time) return prev.value;

        for (int i = 1; i < keys.size(); i++) {
            Keyframe next = keys.get(i);
            if (t <= next.time) {
                float span = next.time - prev.time;
                if (span <= 0f) return next.value;
                return Anim.lerp(prev.value, next.value, next.easing.ease((t - prev.time) / span));
            }
            prev = next;
        }
        return keys.getLast().value;
    }

    private record Keyframe(float time, float value, Easing easing) { }
}
