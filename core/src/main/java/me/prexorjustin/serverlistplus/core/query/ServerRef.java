package me.prexorjustin.serverlistplus.core.query;

/**
 * The searchable and sortable parts of a server entry, independent of Minecraft's {@code ServerData}.
 */
public interface ServerRef {
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
}
