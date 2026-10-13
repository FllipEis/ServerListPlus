package me.prexorjustin.serverlistplus.bridge;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.query.Query;
import me.prexorjustin.serverlistplus.core.query.ServerRef;
import me.prexorjustin.serverlistplus.core.sort.ServerSorter;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.view.DividerLayout;
import me.prexorjustin.serverlistplus.core.view.Tabs;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Decides which saved servers are shown and in which order.
 */
public final class ServerView {
    public static final ServerSorter<ServerSelectionList.OnlineServerEntry> SORTER = new ServerSorter<>(
        entry -> ref(entry.getServerData()),
        entry -> {
            Activity activity = activity(entry.getServerData());
            return activity == null ? 0 : activity.lastPlayed();
        },
        entry -> isPinned(entry.getServerData())
    );

    private ServerView() {
    }

    public static SortMode sortMode() {
        return ServerListPlus.store().view().sort();
    }

    public static DetailsMode detailsMode() {
        return ServerListPlus.store().view().details();
    }

    /** Extra data for a server, null if none exists. */
    public static ServerMeta meta(ServerData data) {
        return ServerListPlus.store().get(key(data)).orElse(null);
    }

    /** When and how long the server was played; shared by all entries with its address. */
    public static Activity activity(ServerData data) {
        return ServerListPlus.store().activity(data.ip).orElse(null);
    }

    /** Data of a server, created if it has none yet. */
    public static ServerMeta metaOrCreate(ServerData data) {
        return ServerListPlus.store().getOrCreate(key(data));
    }

    /** Whether the server is pinned in the tab that is shown. */
    public static boolean isPinned(ServerData data) {
        ServerMeta meta = meta(data);
        return meta != null && meta.isPinnedIn(tab());
    }

    public static boolean matches(ServerData data, Query query) {
        if (query.isEmpty()) return true;
        Store store = ServerListPlus.store();
        return query.matches(ref(data), meta(data), id -> store.folder(id).map(Folder::name).orElse(null));
    }

    public static String tab() {
        return ServerListPlus.store().view().tab();
    }

    public static boolean inTab(ServerData data, String tab) {
        return Tabs.contains(ServerListPlus.store(), tab, ref(data), meta(data));
    }

    /**
     * What a tab shows.
     *
     * @param visible     the servers in the tab
     * @param hiddenCount servers hidden from "All" that match the search, listed in a section at its end
     * @param hiddenShown those of them shown because the section is folded out
     */
    public record Arrangement(List<ServerSelectionList.OnlineServerEntry> visible, int hiddenCount,
                              List<ServerSelectionList.OnlineServerEntry> hiddenShown) {
        public static final Arrangement EMPTY = new Arrangement(List.of(), 0, List.of());

        public boolean hasHiddenSection() {
            return hiddenCount > 0;
        }

        public boolean hiddenExpanded() {
            return !hiddenShown.isEmpty();
        }
    }

    /**
     * Filters (tab and search) and sorts the entries vanilla is about to show, in place.
     * Entries that are not shown are closed to release their icon textures.
     * In "All", servers hidden from it go into a section at the end, or inline when searching {@code is:hidden}.
     *
     * @param hiddenExpanded whether that section is folded out
     */
    public static Arrangement arrange(List<ServerSelectionList.OnlineServerEntry> entries, Query query, boolean hiddenExpanded) {
        String tab = tab();
        boolean all = ViewSettings.TAB_ALL.equals(tab);
        List<ServerSelectionList.OnlineServerEntry> visible = SORTER.sorted(entries.stream()
            .filter(entry -> (inTab(entry.getServerData(), tab) || all && query.wantsHidden()) && matches(entry.getServerData(), query))
            .toList(), sortMode());
        List<ServerSelectionList.OnlineServerEntry> hidden = all && !query.wantsHidden()
            ? SORTER.sorted(entries.stream()
                .filter(entry -> isHiddenInAll(entry.getServerData()) && matches(entry.getServerData(), query))
                .toList(), sortMode())
            : List.of();
        List<ServerSelectionList.OnlineServerEntry> hiddenShown = hiddenExpanded ? hidden : List.of();

        Set<ServerSelectionList.OnlineServerEntry> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        keep.addAll(visible);
        keep.addAll(hiddenShown);
        for (ServerSelectionList.OnlineServerEntry entry : entries) {
            if (!keep.contains(entry)) entry.close();
        }
        entries.clear();
        entries.addAll(visible);
        entries.addAll(hiddenShown);
        return new Arrangement(visible, hidden.size(), hiddenShown);
    }

    public static boolean isHiddenInAll(ServerData data) {
        ServerMeta meta = meta(data);
        return meta != null && meta.hiddenInAll();
    }

    /** Key of a server; see {@link ServerKeys}. */
    public static String key(ServerData data) {
        return ServerKeys.of(data);
    }

    /** Dividers only make sense in the user's own order, not in a sorted or searched list. */
    public static boolean dividersShown(Query query) {
        return query.isEmpty() && sortMode() == SortMode.MANUAL;
    }

    /** The shown servers with the dividers of the current tab between them. */
    public static List<DividerLayout.Row<ServerSelectionList.OnlineServerEntry>> withDividers(
        List<ServerSelectionList.OnlineServerEntry> shown, Query query
    ) {
        List<Divider> dividers = dividersShown(query) ? ServerListPlus.store().dividers(tab()) : List.of();
        return DividerLayout.layout(shown, entry -> key(entry.getServerData()), dividers);
    }

    /** True while the list shows every server in vanilla order, so vanilla's index-based moves are correct. */
    public static boolean showsVanillaOrder(ServerList servers, Query query) {
        if (!query.isEmpty() || sortMode() != SortMode.MANUAL || !ViewSettings.TAB_ALL.equals(tab())) return false;
        if (!ServerListPlus.store().dividers(tab()).isEmpty()) return false;
        for (int i = 0; i < servers.size(); i++) {
            if (isPinned(servers.get(i)) || isHiddenInAll(servers.get(i))) return false;
        }
        return true;
    }

    /** Rebuilds the visible list and keeps the selected server selected if it is still shown. */
    public static void refresh(ServerSelectionList list, ServerList servers) {
        ServerSelectionList.Entry selected = list.getSelected();
        list.updateOnlineServers(servers);
        if (selected instanceof ServerSelectionList.OnlineServerEntry online) {
            select(list, online.getServerData());
        } else if (selected != null && !list.children().contains(selected)) {
            list.setSelected(null);
        }
        // A shorter list must not stay scrolled past its end
        //? if >=1.21.4 {
        list.setScrollAmount(list.scrollAmount());
        //?} else
        /*list.setScrollAmount(list.getScrollAmount());*/
    }

    /** Rebuilds the list for a different set of servers (tab, search) and starts at the top. */
    public static void showFromTop(ServerSelectionList list, ServerList servers) {
        refresh(list, servers);
        list.setScrollAmount(0);
    }

    public static void select(ServerSelectionList list, ServerData data) {
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData() == data) {
                list.setSelected(online);
                return;
            }
        }
        list.setSelected(null);
    }

    public static ServerRef ref(ServerData data) {
        return new ServerRef() {
            @Override
            public String name() {
                return data.name;
            }

            @Override
            public String address() {
                return data.ip;
            }

            @Override
            public String motd() {
                return data.motd == null ? "" : data.motd.getString();
            }

            @Override
            public long ping() {
                return data.state() == ServerData.State.SUCCESSFUL ? data.ping : -1;
            }

            @Override
            public int onlinePlayers() {
                return data.state() == ServerData.State.SUCCESSFUL && data.players != null ? data.players.online() : -1;
            }
        };
    }
}
