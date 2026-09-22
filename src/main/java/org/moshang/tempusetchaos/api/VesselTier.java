package org.moshang.tempusetchaos.api;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

@Accessors(fluent = true)
public enum VesselTier implements StringRepresentable {
    IRON("iron", 51200, 100, 10000),
    REINFORCED("reinforced", 102400, 50, 15000),
    OBSIDIAN("obsidian", 204800, 33, 20000),
    NETHERITE("netherite", 409600, 25, 30000),
    CREATIVE("creative", Integer.MAX_VALUE, 0, Integer.MAX_VALUE);

    private final String name;
    @Getter
    private final int capacity;
    @Getter
    private final int increasement;
    @Getter
    private final int durability;

    VesselTier(String name, int capacity, int increasement, int durability) {
        this.name = name;
        this.capacity = capacity;
        this.increasement = increasement;
        this.durability = durability;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return this.name;
    }
}
