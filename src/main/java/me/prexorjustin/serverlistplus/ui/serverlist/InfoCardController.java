package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;

/**
 * The info card with a server's notes and tags: docked right of the list when there is room,
 * then it follows the selected server; otherwise over the list until it is closed.
 * <p>
 * {@link ServerListUi#relayout()} decides where it goes and calls {@link #place}. Edits are
 * written to the server's data while typing, so leaving the screen with the card open keeps them.
 */
final class InfoCardController {
    /** Distance of the card to the screen edges. */
    private static final int MARGIN = 6;

    private final UiContext ui;

    InfoCardController(UiContext ui) {
        this.ui = ui;
    }

    /** How the card goes next to the list: x, y and size of the room for it. */
    record Dock(int x, int y, int width, int height) {
    }

    /** Where a card was asked to go, to rebuild it only when that changes. */
    private record Placement(int x, int y, int width, int maxHeight, boolean docked) {
    }

    /** What a click did while the card is open. */
    enum Press {
        /** Handled; nothing else may react. */
        USED,
        /** On one of the card's input fields, which take it themselves. */
        INPUT,
        /** Next to the docked card, for the rest of the screen. */
        ELSEWHERE
    }

    /** The server the card shows, null while it is closed. */
    private ServerData infoServer;
    private ServerInfoPanel info;
    private Placement placement;

    boolean isOpen() {
        return infoServer != null;
    }

    /** Whether the card lies over the list, so the list must not react to the pointer. */
    boolean isCovering() {
        return info != null && !placement.docked();
    }

    /** Whether the card is docked next to the list, so the list stays usable. */
    boolean isDocked() {
        return info != null && placement.docked();
    }

    ServerData server() {
        return infoServer;
    }

    /** Whether typing goes into the card. */
    boolean hasFocus() {
        return info != null && info.children().contains(ui.screen().getFocused());
    }

    /** Opens the card with the cursor in the notes; also used by the notes symbol in a row. */
    void open(ServerData server) {
        show(server);
        if (info != null) ui.hook().serverlistplus$focus(info.initialFocus());
    }

    /** Shows another server in the card, keeping what was typed for the previous one. */
    void show(ServerData server) {
        if (server == infoServer) return;
        dropPanel();
        infoServer = server;
        // The list makes room for a docked card; the layout then calls place
        ui.relayout();
    }

    /**
     * Puts the card into the room next to the list, or over the list when there is none.
     * Called by the layout; rebuilt only when the place changed, since the text box cannot be resized.
     */
    void place(Dock dock) {
        if (infoServer == null) return;
        Placement wanted;
        if (dock != null) {
            wanted = new Placement(dock.x(), dock.y(), dock.width(), dock.height(), true);
        } else {
            ServerSelectionList list = ui.list();
            int x = list.getX() + (list.getWidth() - ServerInfoPanel.WIDTH) / 2;
            wanted = new Placement(x, list.getY() + MARGIN, ServerInfoPanel.WIDTH, ui.screen().height - 2 * MARGIN, false);
        }
        if (info != null && wanted.equals(placement)) return;

        boolean focused = hasFocus();
        dropPanel();
        placement = wanted;
        info = new ServerInfoPanel(ui.font(), infoServer, ServerView.metaOrCreate(infoServer),
            wanted.x(), wanted.y(), wanted.width(), wanted.maxHeight(), wanted.docked(), this::close);
        if (!wanted.docked()) {
            // On small screens the card moves up over the tabs rather than off the bottom
            int overflow = info.getBottom() - (ui.screen().height - MARGIN);
            if (overflow > 0) {
                info.setY(Math.max(MARGIN, wanted.y() - overflow));
                info.layout();
            }
        }
        ui.hook().serverlistplus$addOverlay(info);
        // In front of the list, which would otherwise take clicks meant for them
        info.children().forEach(ui.hook()::serverlistplus$addInFront);
        if (focused) ui.hook().serverlistplus$focus(info.initialFocus());
    }

    /** Keeps what was typed and removes the card's widgets. */
    private void dropPanel() {
        if (info == null) return;
        ServerInfoPanel panel = info;
        info = null;
        panel.save();
        panel.children().forEach(ui.hook()::serverlistplus$remove);
        ui.hook().serverlistplus$remove(panel);
    }

    /** Keeps what was typed and closes the card, without laying out the screen it is leaving. */
    void closeQuietly() {
        dropPanel();
        infoServer = null;
        placement = null;
    }

    /** Keeps what was typed and closes the card; the list gets its room back. */
    private void close() {
        if (infoServer == null) return;
        dropPanel();
        infoServer = null;
        placement = null;
        ui.changed();
    }

    /**
     * Over the card, clicks go to its fields and tag chips only. A click next to a card over the
     * list closes it; next to a docked card the rest of the screen reacts as usual.
     */
    Press press(Pointer pointer) {
        if (info == null) return Press.ELSEWHERE;
        if (!info.isMouseOver(pointer.x(), pointer.y())) {
            if (placement.docked()) return Press.ELSEWHERE;
            close();
            return Press.USED;
        }
        if (info.isOverInput(pointer.x(), pointer.y())) return Press.INPUT;
        info.clickChip(pointer.x(), pointer.y());
        return Press.USED;
    }

    /** Escape closes the card; Enter adds a tag, but in the notes it starts a new line as usual. */
    boolean key(Key key) {
        if (key == Key.ESCAPE) {
            close();
            return true;
        }
        if (key == Key.CONFIRM && info != null && info.tagInput().isFocused()) {
            info.addTypedTags();
            return true;
        }
        return false;
    }
}
