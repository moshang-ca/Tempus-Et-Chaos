package org.moshang.tempusetchaos.client.gui.anim;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Track {
    private final List<Keyframe> keys = new ArrayList<>();
    private long startMs;
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

    public Track key(float time, float value, Easing easing) {
        keys.add(new Keyframe(Math.clamp(time, 0f, 1f), value, easing));
        keys.sort((a, b) -> Float.compare(a.time, b.time));
        return this;
    }

    public void play(long durationMs, boolean loop, boolean alternate) {
        this.durationMs = Math.max(1, durationMs);
        this.startMs = Anim.now();
        this.loop = loop;
        this.alternate = alternate;
        this.running = true;
        update();
    }

    public void restart() {
        this.startMs = Anim.now();
        this.running = true;
        update();
    }

    public void stop() { running = false; }

    public void snap(float v) {
        this.current = v;
        this.running = false;
    }

    public void update() {
        if (!running) return;
        long elapsed = Anim.now() - startMs;

        if (loop) {
            long total = alternate ? durationMs * 2 : durationMs;
            elapsed %= total;
            if (alternate && elapsed >= durationMs) {
                elapsed = total - elapsed;
            }
        } else if (elapsed >= durationMs) {
            elapsed = durationMs;
            running = false;
        }

        float t = elapsed / (float) durationMs;
        current = sample(t);
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
                float lt = (t - prev.time) / span;
                float eased = next.easing.ease(lt);
                return prev.value + (next.value - prev.value) * eased;
            }
            prev = next;
        }
        return keys.getLast().value;
    }

    private record Keyframe(float time, float value, Easing easing) { }
}
