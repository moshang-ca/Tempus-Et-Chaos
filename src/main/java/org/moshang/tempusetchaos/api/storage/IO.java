package org.moshang.tempusetchaos.api.storage;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum IO implements StringRepresentable {
    INPUT("input"),
    OUTPUT("output"),
    BOTH("both");

    private final String name;
    IO(String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return name;
    }
}
