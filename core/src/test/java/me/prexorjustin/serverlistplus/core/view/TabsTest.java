package me.prexorjustin.serverlistplus.core.view;

import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.query.ServerRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabsTest {
    private record Server(String name, String address, String motd, long ping) implements ServerRef {
    }

    private static final Server FAST = new Server("Fast", "fast.example.com", "", 20);
    private static final Server SLOW = new Server("Slow", "slow.example.com", "", 400);

    @Test
    void allContainsEverything() {
        assertTrue(Tabs.contains(new Store(), ViewSettings.TAB_ALL, FAST, null));
    }

    @Test
    void folderContainsAssignedServers() {
        Store store = new Store();
        Folder folder = new Folder("Friends");
        store.folders().add(folder);
        ServerMeta meta = new ServerMeta();
        assertFalse(Tabs.contains(store, folder.id(), FAST, meta));
        meta.addToFolder(folder.id());
        assertTrue(Tabs.contains(store, folder.id(), FAST, meta));
    }

    @Test
    void smartFolderMatchesItsQuery() {
        Store store = new Store();
        Folder smart = new Folder("Low ping", "ping:<100");
        store.folders().add(smart);
        assertTrue(Tabs.contains(store, smart.id(), FAST, null));
        assertFalse(Tabs.contains(store, smart.id(), SLOW, null));
    }

    @Test
    void unknownTabShowsEverything() {
        assertTrue(Tabs.contains(new Store(), "deleted", FAST, null));
    }

    @Test
    void dropTargets() {
        Store store = new Store();
        Folder normal = new Folder("Friends");
        Folder smart = new Folder("Low ping", "ping:<100");
        store.folders().add(normal);
        store.folders().add(smart);
        assertTrue(Tabs.acceptsDrop(store, ViewSettings.TAB_ALL));
        assertTrue(Tabs.acceptsDrop(store, normal.id()));
        assertFalse(Tabs.acceptsDrop(store, smart.id()));
        assertFalse(Tabs.acceptsDrop(store, "deleted"));
    }

    @Test
    void hiddenServersLeaveAllButNotTheirFolder() {
        Store store = new Store();
        Folder folder = store.addFolder(new Folder("Friends"));
        ServerMeta meta = new ServerMeta();
        meta.addToFolder(folder.id());
        meta.setHiddenInAll(true);
        assertFalse(Tabs.contains(store, ViewSettings.TAB_ALL, FAST, meta));
        assertTrue(Tabs.contains(store, folder.id(), FAST, meta));
    }
}
