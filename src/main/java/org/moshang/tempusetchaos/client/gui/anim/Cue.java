package org.moshang.tempusetchaos.client.gui.anim;

/**
 * When a move happens, and how long it takes: a delay measured from the node's trigger plus a duration, both on the
 * clock {@link Anim} reads. A cue is shared rather than owned, which is what keeps the nodes of one effect on the same
 * window and what makes "after that move is over" something a type expresses.
 */
@SuppressWarnings("unused")
public record Cue(long delayMs, long durationMs, Easing easing) {
    public Cue {
        delayMs = Math.max(0L, delayMs);
        durationMs = Math.max(1L, durationMs);
    }

    public Cue(long delayMs, long durationMs) {
        this(delayMs, durationMs, Easing.EASE_OUT_QUAD);
    }

    /** The cue that starts once {@code previous} is over, after an optional hold. */
    public static Cue after(Cue previous, long holdMs, long durationMs, Easing easing) {
        return new Cue(previous.endMs() + holdMs, durationMs, easing);
    }

    public static Cue startingAt(Cue anchor, long offsetMs, long durationMs, Easing easing) {
        return new Cue(anchor.delayMs + Math.max(0L, offsetMs), durationMs, easing);
    }

    public long endMs() {
        return delayMs + durationMs;
    }

    /**
     * A track shaped for this cue, running {@code from -> to} across its window. Handing the same cue to several
     * nodes is what guarantees they share the shape as well as the timing.
     */
    public Track track(float from, float to) {
        return new Track().key(0f, from).key(1f, to, easing);
    }
}
