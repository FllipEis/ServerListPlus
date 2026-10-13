package me.prexorjustin.serverlistplus.core.sort;

import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.query.ServerRef;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSorterTest {
    private record Server(String name, long ping, int onlinePlayers) implements ServerRef {
        @Override
        public String address() {
            return name.toLowerCase() + ".example.com";
        }

        @Override
        public String motd() {
            return "";
        }
    }

    private static final String TAB = "all";
    private final Map<Server, ServerMeta> metas = new HashMap<>();
    private final Map<Server, Long> lastPlayed = new HashMap<>();
    private final ServerSorter<Server> sorter = new ServerSorter<>(server -> server, server -> lastPlayed.getOrDefault(server, 0L), server -> metas.containsKey(server) && metas.get(server).isPinnedIn(TAB));

    private static final Server ALPHA = new Server("§aAlpha", 120, 10);
    private static final Server BRAVO = new Server("bravo", 30, 500);
    private static final Server CHARLIE = new Server("Charlie", -1, -1);
    private static final Server DELTA = new Server("delta", 30, 0);
    private static final List<Server> MANUAL_ORDER = List.of(DELTA, CHARLIE, BRAVO, ALPHA);

    private ServerMeta meta(Server server) {
        return metas.computeIfAbsent(server, s -> new ServerMeta());
    }

    private List<String> names(SortMode mode) {
        return sorter.sorted(MANUAL_ORDER, mode).stream().map(Server::name).toList();
    }

    @Test
    void manualKeepsOrder() {
        assertEquals(List.of("delta", "Charlie", "bravo", "§aAlpha"), names(SortMode.MANUAL));
    }

    @Test
    void nameIgnoresCaseAndFormatting() {
        assertEquals(List.of("§aAlpha", "bravo", "Charlie", "delta"), names(SortMode.NAME));
    }

    @Test
    void pingAscendingUnknownLastTiesKeepManualOrder() {
        assertEquals(List.of("delta", "bravo", "§aAlpha", "Charlie"), names(SortMode.PING));
    }

    @Test
    void playersDescendingUnknownLast() {
        assertEquals(List.of("bravo", "§aAlpha", "delta", "Charlie"), names(SortMode.PLAYERS));
    }

    @Test
    void lastPlayedNewestFirstNeverLast() {
        lastPlayed.put(ALPHA, 100L);
        lastPlayed.put(CHARLIE, 300L);
        assertEquals(List.of("Charlie", "§aAlpha", "delta", "bravo"), names(SortMode.LAST_PLAYED));
    }

    @Test
    void pinnedAlwaysFirst() {
        meta(CHARLIE).setPinnedIn(TAB, true);
        meta(ALPHA).setPinnedIn(TAB, true);
        meta(BRAVO).setPinnedIn("other tab", true);
        assertEquals(List.of("Charlie", "§aAlpha", "delta", "bravo"), names(SortMode.MANUAL));
        assertEquals(List.of("§aAlpha", "Charlie", "bravo", "delta"), names(SortMode.NAME));
        assertEquals(List.of("§aAlpha", "Charlie", "delta", "bravo"), names(SortMode.PING));
    }

    @Test
    void isSorted() {
        assertTrue(sorter.isSorted(MANUAL_ORDER, SortMode.MANUAL));
        assertFalse(sorter.isSorted(MANUAL_ORDER, SortMode.NAME));
        assertTrue(sorter.isSorted(sorter.sorted(MANUAL_ORDER, SortMode.PLAYERS), SortMode.PLAYERS));
    }
}
