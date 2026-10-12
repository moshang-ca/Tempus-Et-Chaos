package org.moshang.tempusetchaos.client.gui;

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IUiWindow extends IUiNode {
    default boolean handlesOutsideClicks() { return false; }

    @Nullable
    default IUiWindow getParent() { return null; }

    /**
     * The host is laying every window out again: re-place whatever of this window depends on the host size. A window
     * that reads the host size nowhere has nothing to do, which is why the default is empty.
     */
    default void onRelayout(int hostWidth, int hostHeight) {}
}
