package org.moshang.tempusetchaos.api;

public enum EntropyPipeFaceMode {
    NONE,
    EXTRACT,
    OUTPUT;

    public EntropyPipeFaceMode next() {
        return switch (this) {
            case NONE -> EXTRACT;
            case EXTRACT -> OUTPUT;
            case OUTPUT -> NONE;
        };
    }
}
