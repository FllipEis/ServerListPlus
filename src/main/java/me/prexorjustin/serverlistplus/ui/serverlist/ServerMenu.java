package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ui.text.NameText;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.ServerMeta;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The right-click menu of a server.
 */
final class ServerMenu {
    private final UiContext ui;
    private final TabController tabs;
    private final DividerController dividers;
    private final InfoCardController infoCard;

    ServerMenu(UiContext ui, TabController tabs, DividerController dividers, InfoCardController infoCard) {
        this.ui = ui;
        this.tabs = tabs;
        this.dividers = dividers;
        this.infoCard = infoCard;
    }

    void open(ServerData server, int x, int y) {
        ServerMeta meta = ServerView.meta(server);
        boolean pinned = ServerView.isPinned(server);
        boolean hidden = meta != null && meta.hiddenInAll();

        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(NameText.of(server.name)));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.join"), () -> ui.hook().serverlistplus$join(server)));
        items.add(ContextMenu.Item.action(Component.translatable(pinned ? "serverlistplus.unpin" : "serverlistplus.pin"),
            () -> setPinned(server, !pinned)));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.menu.folders"), folderItems(server, meta)));
        items.add(ContextMenu.Item.action(Component.translatable(hidden ? "serverlistplus.menu.show_in_all" : "serverlistplus.menu.hide_in_all"), () -> {
            ServerView.metaOrCreate(server).setHiddenInAll(!hidden);
            ui.changed();
        }));
        items.add(new ContextMenu.Item(Component.translatable("serverlistplus.menu.add_divider"),
            () -> dividers.addAbove(ServerView.key(server)), List.of(), ServerView.dividersShown(ui.hook().serverlistplus$query())));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.info"), () -> infoCard.open(server)));
        ui.openMenu(items, x, y);
    }

    /** A server can be in any number of folders; each entry adds or removes one. */
    private List<ContextMenu.Item> folderItems(ServerData server, ServerMeta meta) {
        List<ContextMenu.Item> items = new ArrayList<>();
        for (Folder folder : ui.store().folders()) {
            if (folder.isSmart()) continue;
            boolean inFolder = meta != null && meta.isInFolder(folder.id());
            items.add(ContextMenu.Item.action(Component.literal(folder.name()),
                () -> setInFolder(server, folder, !inFolder)).checked(inFolder));
        }
        if (!items.isEmpty()) items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.new_folder"), () -> {
            Folder folder = ui.store().addFolder(new Folder(tabs.uniqueFolderName()));
            setInFolder(server, folder, true);
            tabs.startEditing(folder, false);
        }));
        return items;
    }

    private void setInFolder(ServerData server, Folder folder, boolean inFolder) {
        ServerMeta meta = ServerView.metaOrCreate(server);
        if (inFolder) meta.addToFolder(folder.id());
        else meta.removeFromFolder(folder.id());
        ui.changed();
        ServerView.select(ui.list(), server);
    }

    /** Pins or unpins in the tab that is shown. */
    private void setPinned(ServerData server, boolean pinned) {
        ServerView.metaOrCreate(server).setPinnedIn(ServerView.tab(), pinned);
        ui.changed();
        ServerView.select(ui.list(), server);
    }
}
