package org.moshang.tempusetchaos.api;

import org.moshang.tempusetchaos.util.MatchSet;

public interface IMatchable {
    void addMatchSet(MatchSet.Entry entry);
    void removeMatchSet(MatchSet.Entry entry);
    default void modifyMatchSet(MatchSet.Entry entry, int modifier) {
        if (modifier == 0) {
            removeMatchSet(entry);
        } else if (modifier == 1) {
            addMatchSet(entry);
        } else {
            throw new IllegalArgumentException("Unknown modifier mode: " + modifier);
        }
    }
}
