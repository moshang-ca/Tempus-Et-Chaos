package org.moshang.tempusetchaos.client.gui.anim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The animations a node owns together with the props they write into.
 * Order decides everything: a later animation overwrites what an earlier one wrote, conflicts are not
 * arbitrated on purpose.
 * <p>
 * {@link #tick()} and {@link #fire(AnimEvent, boolean)} walk the list by index, so adding from inside a
 * callback is safe while removing one makes the next entry be skipped. Do not mutate the list there.
 */
@SuppressWarnings("unused")
public final class AnimSet {
    private final AnimProps props = new AnimProps();
    private final List<Animation> animations = new ArrayList<>();
    private final Set<Animation> closing = new LinkedHashSet<>();

    public AnimProps props() {
        return props;
    }

    public List<Animation> animations() {
        return animations;
    }

    public AnimSet add(Animation animation) {
        animations.add(animation);
        return this;
    }

    public boolean remove(Animation animation) {
        return animations.remove(animation);
    }

    public void tick() {
        for (Animation animation : animations) {
            animation.tick(props);
        }
    }

    public void fire(AnimEvent event, boolean active) {
        for (Animation animation : animations) {
            animation.onEvent(event, active);
        }
    }

    /**
     * Fire {@link AnimEvent#CLOSING} and remember which animations that event actually started. Only those
     * keep the node alive, so an animation that was already running (a loop, say) can never block a close.
     */
    public void startClosing() {
        if (!closing.isEmpty()) return;
        boolean[] was = new boolean[animations.size()];
        for (int i = 0; i < animations.size(); i++) {
            was[i] = animations.get(i).isRunning();
        }
        fire(AnimEvent.CLOSING, true);
        for (int i = 0; i < was.length && i < animations.size(); i++) {
            Animation animation = animations.get(i);
            if (!was[i] && animation.isRunning()) closing.add(animation);
        }
    }

    /** Whether an exit animation is still playing, meaning the host must not remove the node yet. */
    public boolean isClosing() {
        for (Animation animation : closing) {
            if (animation.isRunning()) return true;
        }
        return false;
    }
}
