package org.moshang.tempusetchaos.client.gui;

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IUiWindow extends IUiNode {
    default boolean handlesOutsideClicks() { return false; }

    @Nullable
    default IUiWindow getParent() { return null; }
}
