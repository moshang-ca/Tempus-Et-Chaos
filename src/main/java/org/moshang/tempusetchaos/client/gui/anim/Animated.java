package org.moshang.tempusetchaos.client.gui.anim;

import org.moshang.tempusetchaos.client.gui.IUiNode;

/**
 * A node that carries animations and the props they write into, its lifecycle wired into the set.
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
