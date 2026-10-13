package me.prexorjustin.serverlistplus.core.view;

import me.prexorjustin.serverlistplus.core.model.Divider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Puts the dividers of a tab between the servers shown in it.
 */
public final class DividerLayout {
    private DividerLayout() {
    }

    /** Either a server or a divider, in display order. */
    public sealed interface Row<T> permits Server, Line {
    }

    public record Server<T>(T value) implements Row<T> {
    }

    public record Line<T>(Divider divider) implements Row<T> {
    }

    /**
     * Each divider goes directly above its anchor server. Dividers without anchor, or whose anchor
     * is not shown (deleted or moved to another folder), go to the end so they are never lost.
     *
     * @param servers servers in display order
     * @param key     address key of a server
     */
    public static <T> List<Row<T>> layout(List<T> servers, Function<T, String> key, List<Divider> dividers) {
        List<Row<T>> rows = new ArrayList<>();
        List<Divider> remaining = new ArrayList<>(dividers);
        for (T server : servers) {
            String serverKey = key.apply(server);
            remaining.removeIf(divider -> {
                if (!serverKey.equals(divider.beforeKey())) return false;
                rows.add(new Line<>(divider));
                return true;
            });
            rows.add(new Server<>(server));
        }
        remaining.forEach(divider -> rows.add(new Line<>(divider)));
        return rows;
    }

    /**
     * Where a divider anchored to {@code removedKey} should go when that server leaves its place:
     * the server that followed it, or null for the end of the list.
     */
    public static <T> String nextAnchor(List<T> servers, Function<T, String> key, String removedKey) {
        for (int i = 0; i < servers.size() - 1; i++) {
            if (key.apply(servers.get(i)).equals(removedKey)) return key.apply(servers.get(i + 1));
        }
        return null;
    }
}
