package org.moshang.tempusetchaos.api;

import net.minecraft.core.Direction;

public interface ICableConnectable {
    default boolean canConnect(Direction dir) { return true; }
}
