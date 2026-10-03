package org.moshang.tempusetchaos.client.gui.anim;

/**
 * A reusable, attachable animation: it advances a driver and writes the result to a target.
 * Concretes are built by {@link Anims} and attached with {@link AnimSet#add(Animation)}.
 * <p>
 * The surrounding pieces: {@link AnimProps} is the output the host consumes; {@link AnimValue} and
 * {@link Track} are drivers producing one number over time; {@code Animation} binds trigger, driver and
 * write target; {@link AnimSet} is the collection a node owns, where list order decides who writes last;
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

    /** Whether this animation is still moving. Lets the host hold a closing node until its exit animation ends. */
    default boolean isRunning() { return false; }
}
