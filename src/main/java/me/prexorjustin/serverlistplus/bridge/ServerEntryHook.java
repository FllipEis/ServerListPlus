package me.prexorjustin.serverlistplus.bridge;

/**
 * Implemented by {@code ServerSelectionList.OnlineServerEntry} through a mixin.
 */
public interface ServerEntryHook {
    /** Updates the ping icon and its tooltips from the current server state. */
    void serverlistplus$refreshStatus();
}
