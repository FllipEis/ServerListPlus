package me.prexorjustin.serverlistplus.core.key;

/**
 * Identifies a saved server by its normalized address plus its name, so two entries with the
 * same address (e.g. one network listed twice under different names) keep separate data.
 * Entries that match in both are told apart by how many such entries come before them.
 * <p>
 * Keys written before names were part of them are just the address; see {@link #isLegacy}.
 */
public final class ServerKey {
    /** Never part of a normalized address, so the first one separates address and name. */
    private static final char SEPARATOR = '#';
    /** Not typeable in a server name, so it can't clash with one. */
    private static final char OCCURRENCE = '\u001F';

    private ServerKey() {
    }

    public static String of(String address, String name) {
        return AddressKey.normalize(address) + SEPARATOR + (name == null ? "" : name.trim());
    }

    /**
     * @param occurrence how many servers with the same address and name come before this one
     */
    public static String of(String address, String name, int occurrence) {
        String key = of(address, name);
        return occurrence <= 0 ? key : key + OCCURRENCE + (occurrence + 1);
    }

    /** The normalized address part of a key. */
    public static String address(String key) {
        int separator = key.indexOf(SEPARATOR);
        return separator < 0 ? key : key.substring(0, separator);
    }

    /** A key from before names were included: only the address. */
    public static boolean isLegacy(String key) {
        return key.indexOf(SEPARATOR) < 0;
    }
}
