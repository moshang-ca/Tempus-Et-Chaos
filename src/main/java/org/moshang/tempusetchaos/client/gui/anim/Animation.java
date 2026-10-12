package org.moshang.tempusetchaos.client.gui.anim;

/**
 * A reusable, attachable animation: it advances a driver and writes the result to a target.
 * <p>
 * {@link AnimProps} is the output the host consumes; {@link AnimValue} and {@link Track} are drivers producing one
 * number over time; {@link AnimSet} is the collection a node owns, where list order decides who writes last;
 * {@link Animated} is the node contract forwarding lifecycle events into that set.
 */
public interface Animation {
    /** Called once per frame. Writes nothing until this animation has actually been triggered. */
    void tick(AnimProps props);

    /** Called when the bound node sees {@code event}, {@code active} being its new state. */
    default void onEvent(AnimEvent event, boolean active) {}

    /** Run towards the target now, whatever the bound event is. */
    default void play() {}

    /** Run towards the start */
    default void reverse() {}

    /** Jump back to the start value. */
    default void reset() {}

    /**
     * Write the end value right away, without moving through it.
     */
    default void finish(AnimProps props) {
        play();
    }

    /** Whether this animation is still moving. */
    default boolean isRunning() { return false; }
}
