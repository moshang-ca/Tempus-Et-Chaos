package org.moshang.tempusetchaos.api;

/** A store a machine pays a value from and is paid a value into. */
public interface IValueChannel {
    long stored();

    long extract(long amount, boolean simulate);

    long insert(long amount, boolean simulate);
}
