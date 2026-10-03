package org.moshang.tempusetchaos.client.gui.anim;

/** Node lifecycle moments an animation can be bound to. */
public enum AnimEvent {
    ADDED,
    HOVER,
    FOCUS,
    /** Fired when the host is asked to close the node, before it is actually removed. */
    CLOSING
}
