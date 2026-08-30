package org.moshang.tempusetchaos.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ChrononStorage implements IChrononStorage, INBTSerializable<Tag> {
    private final int capacity;
    private int stored;

    public ChrononStorage(int capacity){
        this.capacity = capacity;
    }

    @Override
    public int receiveChronon(int amount, boolean simulate) {
        if (!canReceive() || amount <= 0) return 0;
        int beReceived = Mth.clamp(capacity - stored, 0, amount);

        if (!simulate)
            stored += beReceived;
        return beReceived;
    }

    @Override
    public int extractChronon(int amount, boolean simulate) {
        if (!canExtract() || amount <= 0) return 0;

        int beExtracted = Math.min(stored, amount);
        if (!simulate)
            stored -= beExtracted;
        return beExtracted;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getChrononStored() {
        return stored;
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return IntTag.valueOf(stored);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof IntTag tag)
            stored = tag.getAsInt();
        else
            stored = 0;
    }
}
