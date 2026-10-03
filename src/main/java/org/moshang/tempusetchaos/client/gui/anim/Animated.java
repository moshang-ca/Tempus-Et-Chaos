package org.moshang.tempusetchaos.client.gui.anim;

import org.moshang.tempusetchaos.client.gui.IUiNode;

/**
 * A node that carries animations. Implementing this is optional: a node without it keeps the host's
 * plain "no props" path.
 * <p>
 * It only wires the node's lifecycle into the set, which is what removes the old per-frame
 * "advance the driver, then copy it into props" boilerplate.
 */
public interface Animated extends IUiNode {
    AnimSet anims();

    default Animated add(Animation animation) {
        anims().add(animation);
        return this;
    }

    @Override
    default AnimProps getRenderProps() {
        return anims().props();
    }

    @Override
    default void updateAnimation() {
        anims().tick();
    }

    @Override
    default void onAdded() {
        anims().fire(AnimEvent.ADDED, true);
    }

    @Override
    default void onHoverChanged(boolean hovered) {
        anims().fire(AnimEvent.HOVER, hovered);
    }

    @Override
    default void onFocusChanged(boolean focused) {
        anims().fire(AnimEvent.FOCUS, focused);
    }

    @Override
    default void onClosing() {
        anims().startClosing();
    }

    @Override
    default boolean isClosing() {
        return anims().isClosing();
    }
}
