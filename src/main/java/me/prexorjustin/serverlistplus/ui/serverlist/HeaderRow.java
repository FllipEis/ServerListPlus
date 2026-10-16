package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.sort.SortMode;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import me.prexorjustin.serverlistplus.ui.widget.Glyph;
import me.prexorjustin.serverlistplus.ui.widget.IconButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Search, sort and settings at the right end of the tab row. The search is an icon that turns
 * into a field when clicked and back into the icon once it is left empty.
 */
final class HeaderRow {
    private static final int GAP = 2;
    private static final int SEARCH_MIN_WIDTH = 80;
    private static final int SEARCH_MAX_WIDTH = 150;

    private final UiContext ui;
    private final IconButton searchButton;
    private final IconButton sortButton;
    private final IconButton settingsButton;
    /** The search field is open instead of its icon: while it is in use or holds a search. */
    private boolean searchOpen;
    private boolean tooltipsShown = true;

    HeaderRow(UiContext ui) {
        this.ui = ui;
        this.searchButton = new IconButton(Glyph.SEARCH, Component.translatable("serverlistplus.search.open"), button -> {
            openSearch();
            ui.focusAfterClick(search());
        }, () -> !ui.hook().serverlistplus$query().isEmpty());
        this.sortButton = new IconButton(Glyph.SORT, Component.empty(), this::openSortMenu, () -> ServerView.sortMode() != SortMode.MANUAL);
        this.settingsButton = new IconButton(Glyph.SETTINGS, Component.translatable("serverlistplus.settings"), this::openSettingsMenu, () -> false);
        updateTooltips();
        for (IconButton button : List.of(searchButton, sortButton, settingsButton)) ui.hook().serverlistplus$add(button);
        searchOpen = !ui.hook().serverlistplus$search().isEmpty();
    }

    private EditBox search() {
        return ui.hook().serverlistplus$searchBox();
    }

    /**
     * Places the controls from {@code right} leftwards; the search field takes a third of what
     * is left up to {@code left}.
     *
     * @param shown false while something else (the wallpaper toolbar) takes the row
     * @return where the controls start, so the tabs end before it
     */
    int layout(int left, int right, int y, boolean shown) {
        int x = right;
        for (IconButton button : List.of(settingsButton, sortButton)) {
            x -= IconButton.SIZE;
            button.setPosition(x, y);
            button.visible = button.active = shown;
            x -= GAP;
        }
        EditBox search = search();
        search.setVisible(shown && searchOpen);
        searchButton.visible = searchButton.active = shown && !searchOpen;
        if (searchOpen) {
            int width = Math.clamp((x - left) / 3, SEARCH_MIN_WIDTH, SEARCH_MAX_WIDTH);
            x -= width;
            search.setRectangle(width, 20, x, y);
        } else {
            x -= IconButton.SIZE;
            searchButton.setPosition(x, y);
        }
        return x - GAP;
    }

    /** Turns the search icon into the field and focuses it. */
    void openSearch() {
        searchOpen = true;
        ui.relayout();
        ui.hook().serverlistplus$focus(search());
    }

    private void closeSearch() {
        searchOpen = false;
        ui.relayout();
    }

    void tick() {
        EditBox search = search();
        if (searchOpen && !search.isFocused() && search.getValue().isEmpty()) closeSearch();
    }

    /** Clicking empty space doesn't take the focus away from a text field, so an empty search is closed here. */
    void pressAnywhere(Pointer pointer) {
        EditBox search = search();
        if (!searchOpen || search.isMouseOver(pointer.x(), pointer.y()) || !search.getValue().isEmpty()) return;
        if (search.isFocused()) ui.hook().serverlistplus$focus(null);
        closeSearch();
    }

    /** Escape in the empty search field closes it instead of leaving the screen. */
    boolean key(Key key) {
        EditBox search = search();
        if (key != Key.ESCAPE || !searchOpen || !search.isFocused() || !search.getValue().isEmpty()) return false;
        ui.hook().serverlistplus$focus(null);
        closeSearch();
        return true;
    }

    private static Component sortLabel(SortMode mode) {
        return Component.translatable("serverlistplus.sort." + mode.name().toLowerCase(Locale.ROOT));
    }

    /** Tooltips would cover an open menu, so they are hidden meanwhile. */
    void showTooltips(boolean shown) {
        tooltipsShown = shown;
        updateTooltips();
    }

    /** The sort tooltip names the current order, so it changes with it. */
    void updateTooltips() {
        searchButton.setTooltip(tooltip(Component.translatable("serverlistplus.search.open")));
        sortButton.setTooltip(tooltip(Component.translatable("serverlistplus.sort", sortLabel(ServerView.sortMode()))));
        settingsButton.setTooltip(tooltip(Component.translatable("serverlistplus.settings")));
    }

    private Tooltip tooltip(Component text) {
        return tooltipsShown ? Tooltip.create(text) : null;
    }

    private void openSortMenu(IconButton button) {
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(Component.translatable("serverlistplus.sort.title")));
        items.add(ContextMenu.Item.separator());
        for (SortMode mode : SortMode.values()) {
            items.add(ContextMenu.Item.action(sortLabel(mode), () -> ui.hook().serverlistplus$setSort(mode))
                .checked(mode == ServerView.sortMode()));
        }
        items.add(ContextMenu.Item.separator());
        items.add(ContextMenu.Item.title(Component.translatable("serverlistplus.sort.pinned_first")));
        ui.openMenu(items, button.getX(), button.getBottom() + 2);
    }

    private void openSettingsMenu(IconButton button) {
        ViewSettings view = ui.store().view();
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.title(Component.translatable("serverlistplus.name")));
        items.add(ContextMenu.Item.separator());

        List<ContextMenu.Item> details = new ArrayList<>();
        for (DetailsMode mode : DetailsMode.values()) {
            details.add(ContextMenu.Item.action(Component.translatable("serverlistplus.details." + mode.name().toLowerCase(Locale.ROOT)), () -> {
                view.setDetails(mode);
                ServerListPlus.save();
            }).checked(mode == view.details()));
        }
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.details"), details));

        List<ContextMenu.Item> refresh = new ArrayList<>();
        for (int seconds : ViewSettings.AUTO_REFRESH_SECONDS) {
            Component label = seconds == 0 ? Component.translatable("serverlistplus.auto_refresh.off")
                : Component.translatable("serverlistplus.auto_refresh.every", seconds);
            refresh.add(ContextMenu.Item.action(label, () -> {
                view.setAutoRefreshSeconds(seconds);
                ServerListPlus.save();
            }).checked(seconds == view.autoRefreshSeconds()));
        }
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.auto_refresh"), refresh));

        List<ContextMenu.Item> layouts = new ArrayList<>();
        for (TabLayout layout : TabLayout.values()) {
            layouts.add(ContextMenu.Item.action(Component.translatable("serverlistplus.tab_layout." + layout.name().toLowerCase(Locale.ROOT)), () -> {
                view.setTabLayout(layout);
                ServerListPlus.save();
                ui.relayout();
                ui.tabs().scrollIntoView(ServerView.tab());
            }).checked(layout == view.tabLayout()));
        }
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.tab_layout"), layouts));
        ui.openMenu(items, button.getX(), button.getBottom() + 2);
    }
}
