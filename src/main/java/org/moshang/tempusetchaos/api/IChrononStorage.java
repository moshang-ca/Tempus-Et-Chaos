package org.moshang.tempusetchaos.api;

public interface IChrononStorage {
    long receiveChronon(long amount, boolean simulate);
    long extractChronon(long amount, boolean simulate);

    long getCapacity();
    long getChrononStored();

    default boolean canReceive() { return true; }
    default boolean canExtract() { return true; }
}
