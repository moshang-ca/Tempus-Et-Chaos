package org.moshang.tempusetchaos.client.gui.anim;

import lombok.Getter;
import lombok.experimental.Accessors;

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
    @Accessors(fluent = true) @Getter
    private final AnimProps props = new AnimProps();
    @Accessors(fluent = true) @Getter
    private final List<Animation> animations = new ArrayList<>();
    private final Set<Animation> closing = new LinkedHashSet<>();

    /** Pushed in by the host before every frame. While false the set jumps to the end state instead of moving. */
    private boolean enabled = true;
    /** Whether the end state was already written for this disabled run, so it is written once and not per frame. */
    private boolean settled;

    public AnimSet add(Animation animation) {
        animations.add(animation);
        settled = false;
        return this;
    }

    public boolean remove(Animation animation) {
        return animations.remove(animation);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) settled = false;
    }

    public void tick() {
        if (!enabled) {
            settle();
            return;
        }
        for (Animation animation : animations) {
            animation.tick(props);
        }
    }

    /**
     * Writes the end state once and leaves it there. The animations are not ticked any more, so whatever they would
     * have written last is what the node keeps: skipping them outright would strand the props at their start value,
     * which is invisible for a fade in.
     */
    public void settle() {
        if (settled) return;
        settled = true;
        for (Animation animation : animations) {
            animation.finish(props);
        }
    }

    public void fire(AnimEvent event, boolean active) {
        // an event starts new animations, and those still have to reach their end state while disabled
        if (!enabled) settled = false;
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
