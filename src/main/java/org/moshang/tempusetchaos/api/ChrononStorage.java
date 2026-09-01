package org.moshang.tempusetchaos.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ChrononStorage implements IChrononStorage, INBTSerializable<Tag> {
    private final long capacity;
    private long stored;

    public ChrononStorage(int capacity){
        this.capacity = capacity;
    }

    @Override
    public long receiveChronon(long amount, boolean simulate) {
        if (!canReceive() || amount <= 0) return 0;
        long beReceived = Mth.clamp(capacity - stored, 0, amount);

        if (!simulate)
            stored += beReceived;
        return beReceived;
    }

    @Override
    public long extractChronon(long amount, boolean simulate) {
        if (!canExtract() || amount <= 0) return 0;

        long beExtracted = Math.min(stored, amount);
        if (!simulate)
            stored -= beExtracted;
        return beExtracted;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public long getChrononStored() {
        return stored;
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return LongTag.valueOf(stored);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof LongTag tag)
            stored = tag.getAsInt();
        else
            stored = 0;
    }
}
