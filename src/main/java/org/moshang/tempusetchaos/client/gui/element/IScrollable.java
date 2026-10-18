package org.moshang.tempusetchaos.client.gui.element;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import org.moshang.tempusetchaos.client.gui.IUiNode;
import org.moshang.tempusetchaos.client.gui.anim.Anim;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A node whose content is taller than the box it is drawn in. The host clips the node and pushes the animation switch
 * in; the offset and the wheel feel are the same for every scrollable element.
 */
@ParametersAreNonnullByDefault
public interface IScrollable extends IUiNode {
    float STEP = 12f;

    /** The scroll state this node drives. */
    Scroll getScroll();

    default int getViewportHeight() {
        return getHeight();
    }

    default int getMaxScroll() {
        return Math.max(0, getScroll().getContentHeight() - getViewportHeight());
    }

    /** @return whether the target moved */
    default boolean scrollBy(double amount) {
        return getScroll().by((float) amount, getMaxScroll());
    }

    @Override
    default boolean isMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrollBy(-scrollY * STEP);
    }

    default void updateAnimation() {
        getScroll().advance(getMaxScroll());
    }

    /** Offset, target and content height. */
    final class Scroll {
        /** The share of what is left that one 60 fps frame takes, and the distance below which it is over. */
        private static final float EASE = 0.35f;
        private static final float FRAME_MS = 1000f / 60f;
        private static final float EPSILON = 0.5f;

        @Getter
        private int contentHeight;
        private float offset;
        private float target;
        private long lastMs = Anim.now();
        /** Pushed in by the host like the animation switch: while false the offset jumps instead of easing. */
        @Setter
        private boolean enableEasing = true;

        public int getOffset() {
            return Math.round(offset);
        }

        public void setContentHeight(int contentHeight) {
            this.contentHeight = Math.max(0, contentHeight);
        }

        /** Moves the target; the offset follows in {@link #advance(int)}. */
        public boolean by(float delta, int maxScroll) {
            float next = Mth.clamp(target + delta, 0f, maxScroll);
            if (next == target) return false;
            target = next;
            return true;
        }

        /** One frame of easing.
         * @return whether the offset moved */
        public boolean advance(int maxScroll) {
            target = Mth.clamp(target, 0f, maxScroll);
            long now = Anim.now();
            // the ease is a per-60-fps-frame share, so the step is normalized by the frames that really passed
            float frames = Math.max(0f, (now - lastMs) / FRAME_MS);
            lastMs = now;
            if (enableEasing && Math.abs(target - offset) >= EPSILON) {
                offset += (target - offset) * (1f - (float) Math.pow(1f - EASE, frames));
                return true;
            }
            boolean moved = offset != target;
            offset = target;
            return moved;
        }

        public void reset() {
            this.offset = 0f;
            this.target = 0f;
        }
    }
}
