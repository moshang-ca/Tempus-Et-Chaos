package org.moshang.tempusetchaos.api;

import net.minecraft.core.Direction;

public interface IEntropyPipeConnectable {
    default boolean canPipeConnect(Direction dir) {
        return true;
    }
}
