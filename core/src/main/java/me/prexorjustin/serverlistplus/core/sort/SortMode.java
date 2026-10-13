package me.prexorjustin.serverlistplus.core.sort;

/**
 * How the server list is ordered. Pinned servers always come first.
 */
public enum SortMode {
    /** The vanilla order, which the user arranges with the move buttons. */
    MANUAL,
    NAME,
    /** Lowest ping first. */
    PING,
    /** Most players online first. */
    PLAYERS,
    /** Most recently joined first. */
    LAST_PLAYED;

    /** Whether the order depends on ping results that arrive after the list is shown. */
    public boolean isLive() {
        return this == PING || this == PLAYERS;
    }
}
