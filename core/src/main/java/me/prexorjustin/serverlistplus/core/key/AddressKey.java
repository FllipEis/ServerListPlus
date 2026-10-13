package me.prexorjustin.serverlistplus.core.key;

import java.util.Locale;

/**
 * Turns a server address as typed by the user into a stable lookup key.
 * <p>
 * {@code "Play.Example.com:25565"}, {@code "play.example.com."} and {@code " play.example.com "}
 * all map to {@code "play.example.com"}, so metadata survives cosmetic edits of the address.
 */
public final class AddressKey {
    public static final int DEFAULT_PORT = 25565;

    private AddressKey() {
    }

    public static String normalize(String address) {
        if (address == null) return "";
        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return "";

        String host;
        String port;
        if (value.startsWith("[")) {
            // Bracketed IPv6, optionally with port: [::1] or [::1]:25566
            int end = value.indexOf(']');
            if (end < 0) return value;
            host = value.substring(0, end + 1);
            String rest = value.substring(end + 1);
            port = rest.startsWith(":") ? rest.substring(1) : "";
        } else if (value.indexOf(':') != value.lastIndexOf(':')) {
            // Bare IPv6 without brackets cannot carry a port
            return value;
        } else {
            int colon = value.indexOf(':');
            host = colon < 0 ? value : value.substring(0, colon);
            port = colon < 0 ? "" : value.substring(colon + 1);
        }

        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);

        if (port.isEmpty() || port.equals(String.valueOf(DEFAULT_PORT))) return host;
        return host + ":" + port;
    }
}
