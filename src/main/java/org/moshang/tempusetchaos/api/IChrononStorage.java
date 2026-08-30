package org.moshang.tempusetchaos.api;

public interface IChrononStorage {
    int receiveChronon(int amount, boolean simulate);
    int extractChronon(int amount, boolean simulate);

    int getCapacity();
    int getChrononStored();

    default boolean canReceive() { return true; }
    default boolean canExtract() { return true; }
}
