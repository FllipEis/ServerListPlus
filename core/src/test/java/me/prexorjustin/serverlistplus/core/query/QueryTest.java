package me.prexorjustin.serverlistplus.core.query;

import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryTest {
    private record Server(String name, String address, String motd, long ping, int onlinePlayers) implements ServerRef {
        Server(String name, String address, String motd) {
            this(name, address, motd, -1, -1);
        }
    }

    private static final Server HYPIXEL = new Server("Hypixel Network", "mc.hypixel.net", "§aHypixel §cSKYBLOCK §7update", 80, 26000);
    private static final Server LOCAL = new Server("Test", "localhost:25566", "");
    private static final Function<String, String> FOLDERS = Map.of("f1", "Friends")::get;

    private static boolean matches(String query, Server server, ServerMeta meta) {
        return Query.parse(query).matches(server, meta, FOLDERS);
    }

    private static boolean matches(String query, Server server) {
        return matches(query, server, null);
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(Query.parse("").isEmpty());
        assertTrue(Query.parse("   ").isEmpty());
        assertTrue(Query.parse(null).isEmpty());
        assertTrue(matches("", HYPIXEL));
    }

    @Test
    void textMatchesNameAddressAndMotdIgnoringCase() {
        assertTrue(matches("hypixel", HYPIXEL));
        assertTrue(matches("NETWORK", HYPIXEL));
        assertTrue(matches("mc.hyp", HYPIXEL));
        assertTrue(matches("skyblock", HYPIXEL));
        assertFalse(matches("bedwars", HYPIXEL));
    }

    @Test
    void formattingCodesAreIgnored() {
        assertTrue(matches("hypixel skyblock", HYPIXEL));
        assertTrue(Query.parse("§a").isEmpty(), "a lone formatting code is no search term");
    }

    @Test
    void allTermsMustMatch() {
        assertTrue(matches("hypixel net", HYPIXEL));
        assertFalse(matches("hypixel localhost", HYPIXEL));
    }

    @Test
    void quotedPhrase() {
        assertTrue(matches("\"hypixel network\"", HYPIXEL));
        assertFalse(matches("\"network hypixel\"", HYPIXEL));
    }

    @Test
    void negation() {
        assertFalse(matches("-hypixel", HYPIXEL));
        assertTrue(matches("-hypixel", LOCAL));
        assertTrue(matches("-", HYPIXEL) == matches("-", LOCAL), "lone dash is plain text");
    }

    @Test
    void addressWithPortIsText() {
        assertTrue(matches("localhost:25566", LOCAL));
        assertTrue(matches(":25566", LOCAL));
    }

    @Test
    void tagFilter() {
        ServerMeta meta = new ServerMeta();
        meta.setTags(List.of("PvP", "Survival"));
        assertTrue(matches("tag:pvp", HYPIXEL, meta));
        assertTrue(matches("tag:surv", HYPIXEL, meta));
        assertFalse(matches("tag:creative", HYPIXEL, meta));
        assertFalse(matches("tag:pvp", HYPIXEL, null));
        assertTrue(matches("-tag:creative", HYPIXEL, meta));
    }

    @Test
    void folderFilter() {
        ServerMeta meta = new ServerMeta();
        meta.addToFolder("f1");
        assertTrue(matches("folder:friend", HYPIXEL, meta));
        assertFalse(matches("folder:work", HYPIXEL, meta));

        meta.clearFolders();
        meta.addToFolder("unknown");
        assertFalse(matches("folder:friend", HYPIXEL, meta));
    }

    @Test
    void quotedFilterValue() {
        ServerMeta meta = new ServerMeta();
        meta.setTags(List.of("mini games"));
        assertTrue(matches("tag:\"mini g\"", HYPIXEL, meta));
    }

    @Test
    void pinnedFilter() {
        ServerMeta meta = new ServerMeta();
        assertFalse(matches("is:pinned", HYPIXEL, meta));
        meta.setPinnedIn("some-folder", true);
        assertTrue(matches("is:pinned", HYPIXEL, meta));
        assertFalse(matches("-is:pinned", HYPIXEL, meta));
    }

    @Test
    void emptyFilterValuesAreIgnored() {
        assertTrue(Query.parse("tag: folder:").isEmpty());
    }

    @Test
    void unknownIsValueIsText() {
        assertFalse(matches("is:favorite", HYPIXEL));
    }

    @Test
    void pingAndPlayerComparisons() {
        assertTrue(matches("ping:<100", HYPIXEL));
        assertFalse(matches("ping:<50", HYPIXEL));
        assertTrue(matches("ping:>50", HYPIXEL));
        assertTrue(matches("ping:80", HYPIXEL));
        assertTrue(matches("ping:=80", HYPIXEL));
        assertTrue(matches("players:>1000", HYPIXEL));
        assertFalse(matches("players:<1000", HYPIXEL));
    }

    @Test
    void unknownPingNeverMatchesComparison() {
        assertFalse(matches("ping:<1000", LOCAL));
        assertTrue(matches("-ping:<1000", LOCAL));
    }

    @Test
    void invalidComparisonIsText() {
        assertFalse(matches("ping:fast", HYPIXEL));
    }

    @Test
    void domainMatchesDomainAndSubdomains() {
        Server root = new Server("Root", "simplecloud.app", "");
        Server lobby = new Server("Lobby", "Play.SimpleCloud.app:25566", "");
        Server other = new Server("Other", "notsimplecloud.app", "visit simplecloud.app");
        assertTrue(matches("domain:simplecloud.app", root));
        assertTrue(matches("domain:simplecloud.app", lobby));
        assertFalse(matches("domain:simplecloud.app", other), "MOTD and look-alike domains do not count");
    }

    @Test
    void domainWildcards() {
        Server root = new Server("Root", "simplecloud.app", "");
        Server lobby = new Server("Lobby", "play.simplecloud.app", "");
        assertFalse(matches("domain:*.simplecloud.app", root));
        assertTrue(matches("domain:*.simplecloud.app", lobby));
        assertTrue(matches("domain:play.*", lobby));
        assertFalse(matches("domain:play.*", root));
        assertTrue(matches("host:*cloud*", lobby));
        assertTrue(matches("-domain:hypixel.net", lobby));
    }

    @Test
    void domainIgnoresPortAndIpv6() {
        assertTrue(matches("domain:localhost", LOCAL));
        assertTrue(matches("domain:[::1]", new Server("v6", "[::1]:25565", "")));
    }

    @Test
    void tokenizer() {
        assertEquals(List.of("a", "b c", "tag:d e"), Query.tokenize("  a \"b c\"  tag:\"d e\" "));
    }

    @Test
    void hiddenFilter() {
        ServerMeta meta = new ServerMeta();
        assertFalse(matches("is:hidden", HYPIXEL, meta));
        meta.setHiddenInAll(true);
        assertTrue(matches("is:hidden", HYPIXEL, meta));
        assertTrue(Query.parse("is:hidden").wantsHidden());
        assertFalse(Query.parse("-is:hidden").wantsHidden());
        assertFalse(Query.parse("hidden").wantsHidden());
    }

    private record Tracked(ServerRef.Status status, long lastPlayed, long playtimeMs) implements ServerRef {
        @Override
        public String name() {
            return "Tracked";
        }

        @Override
        public String address() {
            return "tracked.example.com";
        }

        @Override
        public String motd() {
            return "";
        }
    }

    private static final long NOW = 1_000_000_000_000L;
    private static final long HOUR = 3_600_000;
    private static final long DAY = 24 * HOUR;

    private static boolean matchesAt(String query, ServerRef server) {
        return Query.parse(query).matches(server, null, FOLDERS, NOW);
    }

    @Test
    void textAlsoMatchesNotes() {
        ServerMeta meta = new ServerMeta();
        meta.setNotes("Base at §a-120 64 300, ask Steve");
        assertTrue(matches("steve", HYPIXEL, meta));
        assertTrue(matches("note:\"-120 64\"", HYPIXEL, meta));
        assertFalse(matches("note:hypixel", HYPIXEL, meta), "note: looks only at the notes");
        assertFalse(matches("note:steve", HYPIXEL, null));
    }

    @Test
    void hasFilters() {
        ServerMeta meta = new ServerMeta();
        assertFalse(matches("has:notes", HYPIXEL, meta));
        assertFalse(matches("has:tags", HYPIXEL, meta));
        assertFalse(matches("has:folder", HYPIXEL, null));
        meta.setNotes("  ");
        assertFalse(matches("has:notes", HYPIXEL, meta), "blank notes are no notes");
        meta.setNotes("x");
        meta.setTags(List.of("pvp"));
        meta.addToFolder("f1");
        assertTrue(matches("has:notes has:tags has:folder", HYPIXEL, meta));
        assertTrue(matches("-has:notes", HYPIXEL, null));
        assertFalse(matches("has:everything", HYPIXEL, meta), "unknown has: value is text");
    }

    @Test
    void onlineAndOffline() {
        Tracked online = new Tracked(ServerRef.Status.ONLINE, 0, 0);
        Tracked offline = new Tracked(ServerRef.Status.OFFLINE, 0, 0);
        Tracked unknown = new Tracked(ServerRef.Status.UNKNOWN, 0, 0);
        assertTrue(matchesAt("is:online", online));
        assertFalse(matchesAt("is:online", offline));
        assertTrue(matchesAt("is:offline", offline));
        assertFalse(matchesAt("is:online", unknown));
        assertFalse(matchesAt("is:offline", unknown), "not pinged yet is neither");
    }

    @Test
    void playtimeComparison() {
        Tracked twoHours = new Tracked(ServerRef.Status.UNKNOWN, NOW - DAY, 2 * HOUR + 1);
        Tracked never = new Tracked(ServerRef.Status.UNKNOWN, 0, 0);
        assertTrue(matchesAt("played:>2h", twoHours));
        assertFalse(matchesAt("played:>3h", twoHours));
        assertTrue(matchesAt("played:<130min", twoHours));
        assertTrue(matchesAt("played:<1d", twoHours));
        assertTrue(matchesAt("played:0", never));
        assertFalse(matchesAt("played:0", twoHours));
        assertTrue(matchesAt("playtime:>0", twoHours));
    }

    @Test
    void lastJoinedComparison() {
        Tracked yesterday = new Tracked(ServerRef.Status.UNKNOWN, NOW - DAY, HOUR);
        Tracked longAgo = new Tracked(ServerRef.Status.UNKNOWN, NOW - 400 * DAY, HOUR);
        Tracked never = new Tracked(ServerRef.Status.UNKNOWN, 0, 0);
        assertTrue(matchesAt("last:<7d", yesterday));
        assertFalse(matchesAt("last:<7d", longAgo));
        assertTrue(matchesAt("last:>1y", longAgo));
        assertTrue(matchesAt("last:>2w", longAgo));
        assertTrue(matchesAt("last:<2mo", yesterday));
        assertFalse(matchesAt("last:<100y", never), "never joined has no last join");
        assertTrue(matchesAt("-last:<100y", never));
    }

    @Test
    void durationsNeedAUnit() {
        assertFalse(matchesAt("played:>5", new Tracked(ServerRef.Status.UNKNOWN, 0, 10 * HOUR)), "a bare number is text");
        assertFalse(matchesAt("last:<soon", new Tracked(ServerRef.Status.UNKNOWN, NOW, 0)));
    }
}
