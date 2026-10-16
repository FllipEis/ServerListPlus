package me.prexorjustin.serverlistplus.core.query;

/**
 * The searchable and sortable parts of a server entry, independent of Minecraft's {@code ServerData}.
 */
public interface ServerRef {
    /** Result of the last status ping. */
    enum Status { UNKNOWN, ONLINE, OFFLINE }

    String name();

    String address();

    /** Plain-text MOTD, empty until the server was pinged. */
    String motd();

    /** Round-trip time in milliseconds, -1 while unknown or unreachable. */
    default long ping() {
        return -1;
    }

    /** Players currently online, -1 while unknown or unreachable. */
    default int onlinePlayers() {
        return -1;
    }

    /** {@link Status#UNKNOWN} until a ping answered or failed. */
    default Status status() {
        return Status.UNKNOWN;
    }

    /** Epoch millis of the last join, 0 if never joined. */
    default long lastPlayed() {
        return 0;
    }

    default long playtimeMs() {
        return 0;
    }
}
