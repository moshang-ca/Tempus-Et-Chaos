package org.moshang.tempusetchaos.client.gui.anim;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class Anims {
    private Anims() {}

    /** Tween writing into the host consumed props. */
    public static Animation tween(AnimEvent event, AnimProps.Writer writer, float from, float to, long durationMs, Easing easing) {
        return new Tween(new AnimValue(from), writer, event, from, to, 0L, durationMs, easing);
    }

    /** Tween driving a node owned value that node reads itself. The value is left at its initial state. */
    public static Animation tween(AnimEvent event, AnimValue value, float from, float to, long durationMs, Easing easing) {
        return new Tween(value, null, event, from, to, 0L, durationMs, easing);
    }

    public static Animation tween(AnimEvent event, Cue cue, AnimProps.Writer writer, float from, float to) {
        return new Tween(new AnimValue(from), writer, event, from, to, cue.delayMs(), cue.durationMs(), cue.easing());
    }

    public static Animation tween(AnimEvent event, Cue cue, AnimValue value, float from, float to) {
        return new Tween(value, null, event, from, to, cue.delayMs(), cue.durationMs(), cue.easing());
    }

    /** Tween bound to no event: started with {@link Animation#play()}, rewound with {@link Animation#reset()}. */
    public static Animation manual(AnimProps.Writer writer, float from, float to, long durationMs, Easing easing) {
        return new Tween(new AnimValue(from), writer, null, from, to, durationMs, 0L, easing);
    }

    public static Animation timeline(AnimEvent event, Track track, long durationMs, boolean loop, boolean alternate, AnimProps.Writer writer) {
        return new Timeline(track, writer, event, 0L, durationMs, loop, alternate);
    }

    /**
     * A timeline placed by a {@link Cue}: it holds its first key until the cue's delay has passed, then moves over
     * the cue's duration. Every node handed the same cue runs on the same window, which is how a multi node effect
     * stays in step without any of them owning the timing.
     */
    public static Animation timeline(AnimEvent event, Cue cue, Track track, AnimProps.Writer writer) {
        return new Timeline(track, writer, event, cue.delayMs(), cue.durationMs(), false, false);
    }

    /** Fade the node in once it is added. */
    public static Animation fadeIn(long durationMs, Easing easing) {
        return tween(AnimEvent.ADDED, AnimProps.Writer.ALPHA, 0f, 1f, durationMs, easing);
    }

    /** Fade the node out while it closes; the host only removes it once this has finished. */
    public static Animation fadeOut(long durationMs, Easing easing) {
        return tween(AnimEvent.CLOSING, AnimProps.Writer.ALPHA, 1f, 0f, durationMs, easing);
    }

    /** Pop the node in once it is added. */
    public static Animation popIn(long durationMs, Easing easing) {
        return tween(AnimEvent.ADDED, AnimProps.Writer.SCALE_XY, 0.9f, 1f, durationMs, easing);
    }

    /** Shrink the node while it closes; the host only removes it once this has finished. */
    public static Animation shrinkOut(long durationMs, Easing easing) {
        return tween(AnimEvent.CLOSING, AnimProps.Writer.SCALE_XY, 1f, 0.9f, durationMs, easing);
    }

    /** Light the node up while hovered, the wash the built-in hover feedback has always used. */
    public static Animation hoverOverlay(float to, long durationMs, Easing easing) {
        return tween(AnimEvent.HOVER, AnimProps.Writer.OVERLAY, 0f, to, durationMs, easing);
    }

    /** Slide the node in along one axis once it is added, settling at the origin. */
    public static Animation slideIn(AnimProps.Writer offset, float from, long durationMs, Easing easing) {
        return tween(AnimEvent.ADDED, offset, from, 0f, durationMs, easing);
    }

    /** Breathe between two values for as long as the node lives. */
    public static Animation pulse(AnimProps.Writer writer, float from, float to, long periodMs, Easing easing) {
        Track track = new Track().key(0f, from, easing).key(0.5f, to, easing).key(1f, from, easing);
        return timeline(AnimEvent.ADDED, track, periodMs, true, false, writer);
    }

    private static final class Tween implements Animation {
        private final AnimValue value;
        @Nullable
        private final AnimProps.Writer writer;
        @Nullable
        private final AnimEvent event;
        private final float from;
        private final float to;
        private final long delayMs;
        private final long durationMs;
        private final Easing easing;
        /** Until a trigger arrives this animation stays out of the way and writes nothing. */
        private boolean started;

        Tween(AnimValue value, @Nullable AnimProps.Writer writer, @Nullable AnimEvent event,
              float from, float to, long delayMs, long durationMs, Easing easing) {
            this.value = value;
            this.writer = writer;
            this.event = event;
            this.from = from;
            this.to = to;
            this.durationMs = durationMs;
            this.delayMs = delayMs;
            this.easing = easing;
        }

        @Override
        public void tick(AnimProps props) {
            if (!started) return;
            value.update();
            if (writer != null) writer.write(props, value.getValue());
        }

        @Override
        public void onEvent(AnimEvent event, boolean active) {
            if (this.event != event) return;
            started = true;
            value.to(active ? to : from, delayMs, durationMs, easing);
        }

        @Override
        public void play() {
            started = true;
            value.to(to, delayMs, durationMs, easing);
        }

        @Override
        public void reverse() {
            started = true;
            value.to(from, delayMs, durationMs, easing);
        }

        @Override
        public void reset() {
            started = true;
            value.snap(from);
        }

        @Override
        public void finish(AnimProps props) {
            // an animation that was never triggered has no end state to jump to: an exit tween on a node that is
            // merely being added would otherwise write its exit value, which for fadeOut is an invisible node
            if (!started) return;
            value.snap(to);
            if (writer != null) writer.write(props, value.getValue());
        }

        @Override
        public boolean isRunning() {
            return value.isRunning();
        }
    }

    private static final class Timeline implements Animation {
        private final Track track;
        private final AnimProps.Writer writer;
        @Nullable
        private final AnimEvent event;
        private final long delayMs;
        private final long durationMs;
        private final boolean loop;
        private final boolean alternate;
        /** The last key, read once here because the track stops sampling it back after it finishes. */
        private final float endValue;
        /** Until a trigger arrives this animation stays out of the way and writes nothing. */
        private boolean started;

        Timeline(Track track, AnimProps.Writer writer, @Nullable AnimEvent event,
                 long delayMs, long durationMs, boolean loop, boolean alternate) {
            this.track = track;
            this.writer = writer;
            this.event = event;
            this.delayMs = delayMs;
            this.durationMs = durationMs;
            this.loop = loop;
            this.alternate = alternate;
            this.endValue = track.endValue();
        }

        @Override
        public void tick(AnimProps props) {
            if (!started) return;
            track.update();
            writer.write(props, track.getCurrent());
        }

        @Override
        public void onEvent(AnimEvent event, boolean active) {
            if (this.event != event) return;
            started = true;
            track.play(delayMs, durationMs, loop, alternate);
        }

        @Override
        public void play() {
            started = true;
            track.play(delayMs, durationMs, loop, alternate);
        }

        @Override
        public void finish(AnimProps props) {
            // same guard as the tween: an exit timeline on a node that is only being added must stay out of the way
            if (!started) return;
            // snap, not stop: stop would leave the track sampling its first key, and the next tick would write that
            // start value straight back over the end value settled here. Snapping also stops a looping track, which
            // never stops itself and would otherwise keep a closing node alive forever.
            track.snap(endValue);
            writer.write(props, endValue);
        }

        @Override
        public boolean isRunning() {
            return track.isRunning();
        }
    }


}
