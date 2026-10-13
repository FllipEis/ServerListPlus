package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ui.NameText;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.core.view.Tabs;
import me.prexorjustin.serverlistplus.ui.DividerEntry;
import me.prexorjustin.serverlistplus.ui.DragGhost;
import me.prexorjustin.serverlistplus.ui.TabStrip;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Dragging a server: onto another server or a divider to reorder, or onto a tab to put it into
 * that folder.
 */
final class ServerDragging {
    /** How far the pointer must move before a press becomes a drag. */
    static final int THRESHOLD = 5;

    private final UiContext ui;

    private ServerSelectionList.OnlineServerEntry pressed;
    private double pressX;
    private double pressY;
    private DragGhost ghost;
    private String dropTab;
    private ServerSelectionList.OnlineServerEntry dropEntry;
    private Divider dropDivider;
    private boolean dropAfter;

    ServerDragging(UiContext ui) {
        this.ui = ui;
    }

    /** Remembers where a press on a server (or empty space, for null) started. */
    void press(ServerSelectionList.OnlineServerEntry entry, Pointer pointer) {
        pressed = entry;
        pressX = pointer.x();
        pressY = pointer.y();
    }

    boolean drag(Pointer pointer) {
        if (pressed == null || !pointer.isLeft()) return false;
        if (ghost == null) {
            if (Math.hypot(pointer.x() - pressX, pointer.y() - pressY) < THRESHOLD) return false;
            ghost = new DragGhost(ui.font(), NameText.of(pressed.getServerData().name));
            ui.hook().serverlistplus$addOverlay(ghost);
        }
        updateTarget(pointer.x(), pointer.y());
        return true;
    }

    boolean release() {
        ServerSelectionList.OnlineServerEntry dragged = pressed;
        pressed = null;
        if (ghost == null || dragged == null) return false;

        ServerData server = dragged.getServerData();
        if (dropTab != null && Tabs.acceptsDrop(ui.store(), dropTab)) {
            dropOnTab(server, dropTab);
        } else if (dropDivider != null) {
            dropOnDivider(server, dropDivider, dropAfter);
        } else if (dropEntry != null) {
            // Dragging is a clear wish for a custom order, so leave any automatic sort order
            if (ServerView.sortMode() != SortMode.MANUAL) ui.hook().serverlistplus$setSort(SortMode.MANUAL);
            ServerListBridge.keepDividersInPlace(ui.list(), server);
            ServerListBridge.moveNextTo(ui.servers(), server, dropEntry.getServerData(), dropAfter);
            ServerListPlus.save();
            ui.hook().serverlistplus$refresh();
            ServerView.select(ui.list(), server);
        }
        ui.hook().serverlistplus$remove(ghost);
        ghost = null;
        dropTab = null;
        dropEntry = null;
        dropDivider = null;
        ui.tabs().setDropTarget(null, false);
        return true;
    }

    private void updateTarget(double x, double y) {
        ServerSelectionList list = ui.list();
        int left = list.getX() + (list.getWidth() - list.getRowWidth()) / 2;
        TabStrip tabs = ui.tabs();
        String tab = tabs.tabAt(x, y);
        dropTab = null;
        dropEntry = null;
        dropDivider = null;
        if (tab != null && !tab.equals(TabStrip.NEW_TAB)) {
            boolean valid = Tabs.acceptsDrop(ui.store(), tab);
            tabs.setDropTarget(tab, valid);
            ghost.setValid(valid);
            ghost.setLine(-1, 0, 0);
            dropTab = tab;
            return;
        }
        tabs.setDropTarget(null, false);

        // Pinned servers stay at the top, so they can't go next to a divider
        if (ServerListBridge.childAt(list, x, y) instanceof DividerEntry divider && !ServerView.isPinned(pressed.getServerData())) {
            int[] row = ServerListBridge.rowBounds(list, divider);
            dropAfter = y > (row[0] + row[1]) / 2.0;
            ghost.setLine(dropAfter ? row[1] + 2 : row[0] - 2, left, left + list.getRowWidth());
            ghost.setValid(true);
            dropDivider = divider.divider();
            return;
        }
        ServerSelectionList.OnlineServerEntry target = ServerListBridge.entryAt(list, x, y);
        if (target != null && target != pressed && canReorder(pressed.getServerData(), target.getServerData())) {
            int[] row = ServerListBridge.rowBounds(list, target);
            dropAfter = y > (row[0] + row[1]) / 2.0;
            ghost.setLine(dropAfter ? row[1] + 2 : row[0] - 2, left, left + list.getRowWidth());
            ghost.setValid(true);
            dropEntry = target;
        } else {
            ghost.setLine(-1, 0, 0);
            ghost.setValid(target == null || target == pressed);
        }
    }

    /** Pinned servers stay among pinned ones; a drop in a sorted view switches to manual order. */
    private static boolean canReorder(ServerData moved, ServerData target) {
        return ServerView.isPinned(moved) == ServerView.isPinned(target);
    }

    /**
     * Dragged out of a folder, the server moves: it leaves the open folder. Dragged out of "All",
     * it is added to the target folder and stays in the folders it was in.
     */
    private void dropOnTab(ServerData server, String tab) {
        ServerMeta meta = ServerView.metaOrCreate(server);
        String open = ServerView.tab();
        if (!tab.equals(open) && ui.store().folder(open).filter(folder -> !folder.isSmart()).isPresent()) meta.removeFromFolder(open);
        if (!ViewSettings.TAB_ALL.equals(tab)) meta.addToFolder(tab);
        ui.changed();
    }

    /** Upper half: the server goes above the divider; lower half: directly below it. */
    private void dropOnDivider(ServerData server, Divider divider, boolean below) {
        if (ServerView.sortMode() != SortMode.MANUAL) ui.hook().serverlistplus$setSort(SortMode.MANUAL);
        ServerListBridge.keepDividersInPlace(ui.list(), server);
        List<ServerSelectionList.OnlineServerEntry> shown = ServerListBridge.shownServers(ui.list());
        ServerData anchor = null;
        for (ServerSelectionList.OnlineServerEntry entry : shown) {
            if (ServerView.key(entry.getServerData()).equals(divider.beforeKey())) anchor = entry.getServerData();
        }
        if (anchor != null && anchor != server) {
            ServerListBridge.moveNextTo(ui.servers(), server, anchor, false);
        } else if (anchor == null && !shown.isEmpty() && shown.getLast().getServerData() != server) {
            // Divider at the end: move the server behind the last shown server
            ServerListBridge.moveNextTo(ui.servers(), server, shown.getLast().getServerData(), true);
        }
        if (below) divider.setBeforeKey(ServerView.key(server));
        ui.changed();
        ServerView.select(ui.list(), server);
    }
}
