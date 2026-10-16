package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.view.DividerLayout;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.DragGhost;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dividers in the server list: their menu, renaming in place (also by double click) and moving
 * them by drag and drop.
 */
final class DividerController {
    private static final int EDITOR_WIDTH = 160;
    private static final long DOUBLE_CLICK_MILLIS = 400;

    private final UiContext ui;
    private final StyleMenus styles;

    private EditBox editor;
    private Divider edited;

    private Divider lastClicked;
    private long lastClickTime;

    private Divider pressed;
    private double pressX;
    private double pressY;
    private DragGhost ghost;
    private String target;
    private boolean targetValid;

    DividerController(UiContext ui, StyleMenus styles) {
        this.ui = ui;
        this.styles = styles;
    }

    /** Adds a divider above a server in the open tab and lets the user name it right away. */
    void addAbove(String serverKey) {
        Divider divider = new Divider(ServerView.tab(), serverKey);
        ui.store().dividers().add(divider);
        ui.changed();
        startEditing(divider);
    }

    private void openMenu(Divider divider, int x, int y) {
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(divider.label().isEmpty()
            ? Component.translatable("serverlistplus.divider") : Component.literal(divider.label())));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.rename"), () -> startEditing(divider)));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.menu.color"),
            styles.colors(divider.color(), divider::setColor, "serverlistplus.color.tab")));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.remove_divider"), () -> {
            ui.store().dividers().remove(divider);
            ui.changed();
        }));
        ui.openMenu(items, x, y);
    }

    /** Edits a divider title in place, on top of its row. */
    private void startEditing(Divider divider) {
        finishEditing(true);
        DividerEntry entry = entryOf(divider);
        if (entry == null) return;
        edited = divider;
        editor = new EditBox(ui.font(), 0, 0, EDITOR_WIDTH, 20, Component.translatable("serverlistplus.divider"));
        editor.setMaxLength(48);
        editor.setValue(divider.label());
        editor.setHint(Component.translatable("serverlistplus.divider.hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        ServerSelectionList list = ui.list();
        int[] row = ServerListBridge.rowBounds(list, entry);
        editor.setRectangle(EDITOR_WIDTH, 20, list.getX() + (list.getWidth() - EDITOR_WIDTH) / 2, (row[0] + row[1]) / 2 - 10);
        ui.hook().serverlistplus$add(editor);
        ui.hook().serverlistplus$focus(editor);
        ui.focusAfterClick(editor);
    }

    private DividerEntry entryOf(Divider divider) {
        for (ServerSelectionList.Entry entry : ui.list().children()) {
            if (entry instanceof DividerEntry dividerEntry && dividerEntry.divider() == divider) return dividerEntry;
        }
        return null;
    }

    private void finishEditing(boolean commit) {
        if (editor == null) return;
        if (commit) edited.setLabel(editor.getValue());
        ui.hook().serverlistplus$remove(editor);
        editor = null;
        edited = null;
        ui.changed();
    }

    void tick() {
        if (editor != null && !editor.isFocused()) finishEditing(true);
    }

    /** A click elsewhere keeps what was typed. */
    void pressAnywhere(Pointer pointer) {
        if (editor != null && !editor.isMouseOver(pointer.x(), pointer.y())) finishEditing(true);
    }

    boolean key(Key key) {
        if (editor == null || key != Key.CONFIRM && key != Key.ESCAPE) return false;
        finishEditing(key == Key.CONFIRM);
        return true;
    }

    /**
     * Right click opens the menu, a double click renames, a press may start dragging.
     *
     * @return true if vanilla must not handle the click
     */
    boolean press(DividerEntry entry, Pointer pointer) {
        Divider divider = entry.divider();
        if (pointer.isRight()) {
            openMenu(divider, (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (!pointer.isLeft()) return false;
        long now = System.currentTimeMillis();
        if (divider == lastClicked && now - lastClickTime < DOUBLE_CLICK_MILLIS) {
            lastClicked = null;
            startEditing(divider);
            return true;
        }
        lastClicked = divider;
        lastClickTime = now;
        pressed = divider;
        pressX = pointer.x();
        pressY = pointer.y();
        return false;
    }

    /** A press that did not hit a divider. */
    void pressElsewhere() {
        pressed = null;
    }

    boolean isDragging() {
        return pressed != null;
    }

    /** Shows where the divider would go: above a server, above another divider, or at the end. */
    boolean drag(Pointer pointer) {
        if (pressed == null || !pointer.isLeft()) return false;
        if (ghost == null) {
            if (Math.hypot(pointer.x() - pressX, pointer.y() - pressY) < ServerDragging.THRESHOLD) return false;
            String label = pressed.label().isEmpty() ? "──────" : "── " + pressed.label() + " ──";
            ghost = new DragGhost(ui.font(), Component.literal(label));
            ui.hook().serverlistplus$addOverlay(ghost);
        }
        ServerSelectionList list = ui.list();
        ServerSelectionList.Entry over = ServerListBridge.childAt(list, pointer.x(), pointer.y());
        int left = list.getX() + (list.getWidth() - list.getRowWidth()) / 2;
        targetValid = true;
        if (over instanceof ServerSelectionList.OnlineServerEntry online) {
            int[] row = ServerListBridge.rowBounds(list, online);
            boolean after = pointer.y() > (row[0] + row[1]) / 2.0;
            List<ServerSelectionList.OnlineServerEntry> shown = ServerListBridge.shownServers(list);
            String key = ServerView.key(online.getServerData());
            target = after ? DividerLayout.nextAnchor(shown, entry -> ServerView.key(entry.getServerData()), key) : key;
            ghost.setLine(after ? row[1] + 2 : row[0] - 2, left, left + list.getRowWidth());
        } else if (over instanceof DividerEntry other && other.divider() != pressed) {
            target = other.divider().beforeKey();
            int[] row = ServerListBridge.rowBounds(list, other);
            ghost.setLine(row[0] - 2, left, left + list.getRowWidth());
        } else if (over == null && list.isMouseOver(pointer.x(), pointer.y())) {
            // Empty space below the servers: the end of the list
            target = null;
            ghost.setLine(-1, 0, 0);
        } else {
            targetValid = false;
            ghost.setLine(-1, 0, 0);
        }
        ghost.setValid(targetValid);
        return true;
    }

    boolean release() {
        Divider divider = pressed;
        pressed = null;
        if (divider == null || ghost == null) return false;
        ui.hook().serverlistplus$remove(ghost);
        ghost = null;
        if (targetValid) {
            divider.setBeforeKey(target);
            ui.changed();
        }
        return true;
    }
}
