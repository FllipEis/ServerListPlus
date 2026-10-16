package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.compat.Screens;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.TabBar;
import me.prexorjustin.serverlistplus.ui.widget.TabSidebar;
import me.prexorjustin.serverlistplus.ui.widget.TabStrip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * The tabs: "All" plus one per folder, shown as a row or as a sidebar. Handles switching,
 * creating, renaming in place, the tab menu and the list of all tabs.
 */
final class TabController implements TabStrip.Listener {
    /** The sidebar is only used while the server list keeps at least this width next to it. */
    private static final int MIN_LIST_WIDTH_WITH_SIDEBAR = 330;
    private static final int TICKS_PER_COUNT_UPDATE = 20;

    private final UiContext ui;
    private final StyleMenus styles;
    private final TabBar bar;
    private final TabSidebar sidebar;
    private boolean sidebarShown;
    private int ticks;

    private EditBox editor;
    private String editedFolder;
    private boolean editsQuery;

    TabController(UiContext ui, StyleMenus styles) {
        this.ui = ui;
        this.styles = styles;
        this.bar = new TabBar(ui.font(), this);
        this.sidebar = new TabSidebar(ui.font(), this);
        ui.hook().serverlistplus$add(bar);
        ui.hook().serverlistplus$add(sidebar);
    }

    /** The tabs currently shown: the sidebar if chosen and there is room, the row otherwise. */
    TabStrip strip() {
        return sidebarShown ? sidebar : bar;
    }

    // ---- Layout ----

    /**
     * Decides between row and sidebar for a list of the given width.
     *
     * @return the width the sidebar takes from the list, 0 for the row
     */
    int chooseLayout(int listWidth, int margin) {
        int width = sidebar.preferredWidth();
        sidebarShown = ui.store().view().tabLayout() == TabLayout.SIDEBAR
            && listWidth - width - margin >= MIN_LIST_WIDTH_WITH_SIDEBAR;
        return sidebarShown ? width + margin : 0;
    }

    void placeSidebar(int x, int y, int height) {
        sidebar.setRectangle(sidebar.preferredWidth(), height, x, y);
    }

    void placeRow(int x, int y, int width) {
        bar.setRectangle(Math.max(0, width), 20, x, y);
    }

    /** @param shown false while something else (the wallpaper toolbar) takes the row */
    void show(boolean shown) {
        bar.visible = shown && !sidebarShown;
        bar.active = bar.visible && editor == null;
        sidebar.visible = shown && sidebarShown;
        sidebar.active = sidebar.visible && editor == null;
        if (editor != null) placeEditor();
    }

    // ---- Tabs ----

    private int count(String tab) {
        int count = 0;
        ServerList servers = ui.servers();
        for (int i = 0; i < servers.size(); i++) {
            if (ServerView.inTab(servers.get(i), tab)) count++;
        }
        return count;
    }

    /** Rebuilds the tabs from the folders; a changed name or count can change the layout. */
    void update() {
        List<TabStrip.Tab> tabs = new ArrayList<>();
        ViewSettings view = ui.store().view();
        tabs.add(tab(ViewSettings.TAB_ALL, Component.translatable("serverlistplus.tab.all"), count(ViewSettings.TAB_ALL), view.allStyle()));
        for (Folder folder : ui.store().folders()) {
            Component label = folder.isSmart()
                ? Component.literal(folder.name()).withStyle(ChatFormatting.ITALIC)
                : Component.literal(folder.name());
            tabs.add(tab(folder.id(), label, count(folder.id()), folder));
        }
        bar.setTabs(tabs, ServerView.tab());
        sidebar.setTabs(tabs, ServerView.tab());
    }

    /** A tab shows its symbol, or a dot in its color if it has a color but no symbol. */
    private static TabStrip.Tab tab(String id, Component label, int count, Styled style) {
        int color = style.color() == null ? 0 : style.color().argb(0xFF);
        String icon = style.icon() != null ? style.icon() : style.color() != null ? "●" : null;
        return new TabStrip.Tab(id, label, count, color, icon);
    }

    /** Counts change while pings arrive (smart folders) and servers are edited elsewhere. */
    void tick() {
        if (editor != null && !editor.isFocused()) finishEditing(true);
        if (++ticks % TICKS_PER_COUNT_UPDATE == 0) {
            update();
            ui.relayout();
        }
    }

    @Override
    public void selectTab(String id) {
        if (id.equals(ServerView.tab())) return;
        ui.store().view().setTab(id);
        ServerListPlus.save();
        ServerView.showFromTop(ui.list(), ui.servers());
        update();
        ui.relayout();
        strip().scrollIntoView(id);
    }

    /** "+": a new folder, or a smart folder from the current search if there is one. */
    @Override
    public void createFolder() {
        String search = ui.hook().serverlistplus$search().trim();
        Folder folder = ui.store().addFolder(search.isEmpty()
            ? new Folder(uniqueFolderName())
            : new Folder(search, search));
        if (folder.isSmart()) ui.hook().serverlistplus$setSearch("");
        selectTab(folder.id());
        startEditing(folder, false);
    }

    String uniqueFolderName() {
        String base = I18n.get("serverlistplus.folder.new");
        String name = base;
        for (int i = 2; ui.store().folderNamed(name).isPresent(); i++) name = base + " " + i;
        return name;
    }

    @Override
    public void renameTab(String id) {
        ui.store().folder(id).ifPresent(folder -> startEditing(folder, false));
    }

    @Override
    public void openTabMenu(String id, int x, int y) {
        Styled style = ui.store().style(id);
        if (style == null) return;
        Folder folder = ui.store().folder(id).orElse(null);
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(folder != null ? Component.literal(folder.name()) : Component.translatable("serverlistplus.tab.all")));
        items.add(ContextMenu.Item.separator());
        if (folder != null) {
            items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.rename"), () -> startEditing(folder, false)));
            items.add(ContextMenu.Item.action(Component.translatable(folder.isSmart() ? "serverlistplus.menu.edit_search" : "serverlistplus.menu.make_smart"),
                () -> startEditing(folder, true)));
            items.add(ContextMenu.Item.separator());
        }
        boolean isDefault = id.equals(ui.store().view().defaultTab());
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.default_tab"), () -> {
            ui.store().view().setDefaultTab(isDefault ? null : id);
            ui.changed();
        }).checked(isDefault));
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.menu.color"),
            styles.colors(style.color(), style::setColor, "serverlistplus.color.none")));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.menu.icon"), styles.icons(style)));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.menu.background"), styles.backgrounds(id, style)));
        if (folder != null) {
            int index = ui.store().folders().indexOf(folder);
            boolean vertical = strip().vertical();
            items.add(ContextMenu.Item.separator());
            items.add(new ContextMenu.Item(Component.translatable(vertical ? "serverlistplus.menu.move_up" : "serverlistplus.menu.move_left"),
                () -> moveFolder(folder, -1), List.of(), index > 0));
            items.add(new ContextMenu.Item(Component.translatable(vertical ? "serverlistplus.menu.move_down" : "serverlistplus.menu.move_right"),
                () -> moveFolder(folder, 1), List.of(), index < ui.store().folders().size() - 1));
            items.add(ContextMenu.Item.separator());
            items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.menu.delete_folder"), () -> confirmDelete(folder)));
        }
        ui.openMenu(items, x, y);
    }

    @Override
    public void openTabList(int x, int y) {
        List<ContextMenu.Item> items = new ArrayList<>();
        for (TabStrip.Tab tab : strip().tabs()) {
            MutableComponent label = Component.empty();
            if (tab.icon() != null) {
                int color = tab.color() != 0 ? tab.color() : 0xA8A8A8;
                label.append(Component.literal(tab.icon() + " ").withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
            }
            label.append(tab.label()).append(Component.literal(" " + tab.count()).withStyle(ChatFormatting.DARK_GRAY));
            items.add(ContextMenu.Item.action(label, () -> selectTab(tab.id())).checked(tab.id().equals(ServerView.tab())));
        }
        ui.openMenu(items, x, y);
    }

    private void moveFolder(Folder folder, int delta) {
        ui.store().moveFolder(folder.id(), delta);
        ui.changed();
    }

    private void confirmDelete(Folder folder) {
        Screens.open(new ConfirmScreen(
            confirmed -> {
                if (confirmed) {
                    ui.store().removeFolder(folder.id());
                    ServerListPlus.save();
                }
                Screens.open(ui.screen());
            },
            Component.translatable("serverlistplus.folder.delete.title", folder.name()),
            Component.translatable(folder.isSmart() ? "serverlistplus.folder.delete.smart" : "serverlistplus.folder.delete.message")
        ));
    }

    // ---- Renaming in place ----

    /**
     * Renames a tab in place: the tab itself becomes a borderless text field that grows while typing.
     * For the saved search of a smart folder a regular field opens next to the tab instead.
     *
     * @param query edit the saved search instead of the name (turns a normal folder into a smart one)
     */
    void startEditing(Folder folder, boolean query) {
        finishEditing(true);
        editedFolder = folder.id();
        editsQuery = query;
        editor = new EditBox(ui.font(), 0, 0, 100, 20, Component.translatable(query ? "serverlistplus.menu.edit_search" : "serverlistplus.menu.rename"));
        editor.setMaxLength(query ? 256 : 32);
        editor.setValue(query ? (folder.isSmart() ? folder.query() : "") : folder.name());
        if (query) {
            editor.setHint(Component.translatable("serverlistplus.folder.query_hint").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        } else {
            editor.setBordered(false);
            editor.setResponder(value -> placeEditor());
        }
        ui.hook().serverlistplus$add(editor);
        strip().active = false;
        placeEditor();
        strip().scrollIntoView(folder.id());
        placeEditor();
        ui.hook().serverlistplus$focus(editor);
        ui.focusAfterClick(editor);
    }

    private void placeEditor() {
        TabStrip strip = strip();
        if (editsQuery) {
            TabStrip.Slot slot = strip.slot(editedFolder).orElse(null);
            if (slot == null) return;
            int screenWidth = ui.screen().width;
            int width = Math.min(220, screenWidth - 12);
            // Below a tab in the row, beside it in the sidebar
            int x = strip.vertical() ? slot.right() + 4 : slot.x();
            int y = strip.vertical() ? slot.y() + (slot.height() - 20) / 2 : slot.bottom() + 2;
            editor.setRectangle(width, 20, Math.max(6, Math.min(x, screenWidth - 6 - width)), y);
            return;
        }
        // Room for the text plus the cursor; a tab in the row grows with it
        int textWidth = Math.max(ui.font().width(editor.getValue()) + 6, 24);
        strip.setEditing(editedFolder, textWidth);
        TabStrip.Slot slot = strip.slot(editedFolder).orElse(null);
        if (slot == null) return;
        int textY = slot.y() + (slot.height() - 8) / 2;
        editor.setRectangle(strip.editorWidth(editedFolder, textWidth), 10, slot.x() + strip.textInset(editedFolder), textY);
    }

    void finishEditing(boolean commit) {
        if (editor == null) return;
        String value = editor.getValue().trim();
        String folderId = editedFolder;
        boolean query = editsQuery;
        ui.hook().serverlistplus$remove(editor);
        editor = null;
        editedFolder = null;
        bar.setEditing(null, 0);
        sidebar.setEditing(null, 0);
        if (commit) {
            ui.store().folder(folderId).ifPresent(folder -> {
                if (query) folder.setQuery(value);
                else if (!value.isEmpty()) folder.setName(value);
            });
        }
        ui.changed();
    }

    /** A click elsewhere keeps what was typed. */
    void pressAnywhere(Pointer pointer) {
        if (editor != null && !editor.isMouseOver(pointer.x(), pointer.y())) finishEditing(true);
    }

    /** Enter keeps the new name, Escape restores the old one. */
    boolean key(Key key) {
        if (editor == null || key != Key.CONFIRM && key != Key.ESCAPE) return false;
        finishEditing(key == Key.CONFIRM);
        return true;
    }
}
