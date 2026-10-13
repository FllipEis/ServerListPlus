package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.ui.ContextMenu;
import me.prexorjustin.serverlistplus.ui.DividerEntry;
import me.prexorjustin.serverlistplus.ui.HiddenSectionEntry;
import me.prexorjustin.serverlistplus.ui.TabStrip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import java.util.List;

/**
 * Everything ServerListPlus adds to the multiplayer screen: tabs, the header controls, menus,
 * drag and drop and in-place editing. Created anew on every {@code init()}.
 * <p>
 * This class only lays the parts out and hands input to them in a fixed order: an open info
 * card or wallpaper toolbar first, then an open menu, then whatever is under the pointer.
 */
public final class ServerListUi implements UiContext {
    /** Space reserved above the server list for the tab row. */
    public static final int TAB_ROW = 24;
    private static final int MARGIN = 6;

    private final Screen screen;
    private final MultiplayerScreenHook hook;
    private final Font font = Minecraft.getInstance().font;

    private final HeaderRow header;
    private final TabController tabs;
    private final WallpaperAdjuster wallpaper;
    private final DividerController dividers;
    private final ServerDragging dragging;
    private final ServerMenu serverMenu;

    private ContextMenu menu;
    private AbstractWidget focusAfterClick;

    /** Where vanilla put the list, before making room for the tabs; width 0 until known. */
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    public <S extends Screen & MultiplayerScreenHook> ServerListUi(S screen) {
        this.screen = screen;
        this.hook = screen;
        this.wallpaper = new WallpaperAdjuster(this);
        StyleMenus styles = new StyleMenus(this, this::adjustWallpaper);
        this.tabs = new TabController(this, styles);
        this.header = new HeaderRow(this);
        this.dividers = new DividerController(this, styles);
        this.dragging = new ServerDragging(this);
        this.serverMenu = new ServerMenu(this, tabs, dividers);
        tabs.update();
    }

    // ---- UiContext ----

    @Override
    public Screen screen() {
        return screen;
    }

    @Override
    public MultiplayerScreenHook hook() {
        return hook;
    }

    @Override
    public Font font() {
        return font;
    }

    @Override
    public TabStrip tabs() {
        return tabs.strip();
    }

    @Override
    public void changed() {
        ServerListPlus.save();
        hook.serverlistplus$refresh();
        tabs.update();
        relayout();
    }

    @Override
    public void openMenu(List<ContextMenu.Item> items, int x, int y) {
        closeMenu();
        menu = new ContextMenu(font, items, x, y, screen.width, screen.height);
        hook.serverlistplus$addOverlay(menu);
        header.showTooltips(false);
    }

    private void closeMenu() {
        if (menu == null) return;
        hook.serverlistplus$remove(menu);
        menu = null;
        header.showTooltips(true);
    }

    @Override
    public void focusAfterClick(AbstractWidget widget) {
        focusAfterClick = widget;
    }

    // ---- Layout ----

    /** Call right after vanilla positioned the list: makes room for the tabs above or next to it. */
    public void layoutList() {
        ServerSelectionList list = list();
        listX = list.getX();
        listY = list.getY();
        listWidth = list.getWidth();
        listHeight = list.getHeight();
        relayout();
        tabs().scrollIntoView(ServerView.tab());
    }

    /**
     * The tab row (or only the header controls, with the sidebar) above the list; the sidebar,
     * if shown, to the left of it. Can be repeated, e.g. when tab names change.
     */
    @Override
    public void relayout() {
        if (listWidth == 0) return;
        int top = listY + TAB_ROW;
        int height = listHeight - TAB_ROW;
        int sidebarWidth = tabs.chooseLayout(listWidth, MARGIN);
        if (sidebarWidth > 0) tabs.placeSidebar(listX + MARGIN, top, height);
        int x = listX + sidebarWidth;
        int width = listWidth - sidebarWidth;
        ServerSelectionList list = list();
        if (list.getX() != x || list.getY() != top || list.getWidth() != width || list.getHeight() != height) {
            // Since 1.21.9 rows store their position, so x must be known when they are placed
            //? if >=1.21.9 {
            list.updateSizeAndPosition(width, height, x, top);
            //?} else {
            /*list.updateSizeAndPosition(width, height, top);
            list.setX(x);
            *///?}
        }

        int rowY = listY + 2;
        boolean shown = !wallpaper.isActive();
        int controls = header.layout(MARGIN, screen.width - MARGIN, rowY, shown);
        tabs.placeRow(MARGIN, rowY, controls - MARGIN);
        tabs.show(shown);
    }

    private void adjustWallpaper(String tab, Styled style) {
        tabs.finishEditing(true);
        tabs.selectTab(tab);
        wallpaper.start(tab, style, MARGIN, listY + 2);
        relayout();
    }

    // ---- Called by the screen ----

    public void tick() {
        header.tick();
        tabs.tick();
        dividers.tick();
    }

    public void openSearch() {
        header.openSearch();
    }

    public void updateSortTooltip() {
        header.updateTooltips();
    }

    /** @return true if the scroll was used and vanilla must not handle it */
    public boolean scroll(double x, double y, double amount) {
        return wallpaper.scroll(x, y, amount);
    }

    /** @return true if the click was used and vanilla must not handle it */
    public boolean press(Pointer pointer) {
        focusAfterClick = null;
        header.pressAnywhere(pointer);
        if (serverMenu.isInfoOpen()) return serverMenu.pressInfo(pointer);
        if (wallpaper.isActive()) return wallpaper.press(pointer);
        if (menu != null) {
            if (!menu.contains(pointer.x(), pointer.y()) || menu.click(pointer.x(), pointer.y())) closeMenu();
            return true;
        }
        tabs.pressAnywhere(pointer);
        dividers.pressAnywhere(pointer);

        ServerSelectionList.Entry over = ServerListBridge.childAt(list(), pointer.x(), pointer.y());
        if (over instanceof HiddenSectionEntry) {
            if (pointer.isLeft()) {
                hook.serverlistplus$setHiddenExpanded(!hook.serverlistplus$hiddenExpanded());
                hook.serverlistplus$refresh();
            }
            return true;
        }
        if (over instanceof DividerEntry divider) {
            dragging.press(null, pointer);
            return dividers.press(divider, pointer);
        }
        dividers.pressElsewhere();

        ServerSelectionList.OnlineServerEntry entry = over instanceof ServerSelectionList.OnlineServerEntry online ? online : null;
        if (pointer.isRight() && entry != null) {
            list().setSelected(entry);
            serverMenu.open(entry.getServerData(), (int) pointer.x(), (int) pointer.y());
            return true;
        }
        if (pointer.isLeft()) dragging.press(entry, pointer);
        return false;
    }

    /** Call after vanilla handled a click. */
    public void afterClick() {
        if (focusAfterClick != null) hook.serverlistplus$focus(focusAfterClick);
        focusAfterClick = null;
    }

    public boolean drag(Pointer pointer) {
        if (dividers.isDragging()) return dividers.drag(pointer);
        if (wallpaper.drag(pointer)) return true;
        return dragging.drag(pointer);
    }

    public boolean release(Pointer pointer) {
        if (wallpaper.release()) return true;
        if (dividers.isDragging()) return dividers.release();
        return dragging.release();
    }

    /** @return true if the key was used and vanilla must not handle it */
    public boolean key(Key key) {
        if (serverMenu.isInfoOpen()) return serverMenu.keyInfo(key);
        if (wallpaper.key(key) || dividers.key(key) || tabs.key(key)) return true;
        if (menu != null && key == Key.ESCAPE) {
            closeMenu();
            return true;
        }
        return header.key(key);
    }
}
