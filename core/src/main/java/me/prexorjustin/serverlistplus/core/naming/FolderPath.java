package me.prexorjustin.serverlistplus.core.naming;

import java.util.Optional;

/**
 * Lets a server name pick its folder: {@code "Friends/Survival SMP"} means
 * server "Survival SMP" in folder "Friends".
 */
public final class FolderPath {
    private FolderPath() {
    }

    public record Split(String folder, String name) {
    }

    /** Splits at the first slash; empty if there is no folder part or no name left. */
    public static Optional<Split> parse(String input) {
        if (input == null) return Optional.empty();
        int slash = input.indexOf('/');
        if (slash < 0) return Optional.empty();
        String folder = input.substring(0, slash).trim();
        String name = input.substring(slash + 1).trim();
        if (folder.isEmpty() || name.isEmpty()) return Optional.empty();
        return Optional.of(new Split(folder, name));
    }
}
