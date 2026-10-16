package me.prexorjustin.serverlistplus.bridge;

/** Implemented by {@code ServerSelectionList.OnlineServerEntry} via mixin. */
public interface ServerEntryHook {
    /** Updates the ping icon and its tooltips from the current server state. */
    void serverlistplus$refreshStatus();

    /** Whether the point is over the notes symbol drawn in the row. */
    boolean serverlistplus$isOverNotes(double x, double y);
}
