package me.prexorjustin.serverlistplus.ui;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * The folder tabs, either as a row above the server list ({@link TabBar}) or as a column
 * next to it ({@link TabSidebar}).
 */
public abstract class TabStrip extends SlpWidget {
    /** Id of the "+" tab that creates a folder. */
    public static final String NEW_TAB = "+";

    /**
     * @param color ARGB accent color, 0 for none
     * @param icon  symbol shown in the accent color before the label, null for none
     */
    public record Tab(String id, Component label, int count, int color, String icon) {
    }

    public record Slot(String id, int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    public interface Listener {
        void selectTab(String id);

        void createFolder();

        /** Double click on a tab. */
        void renameTab(String id);

        /** Right click on a tab. */
        void openTabMenu(String id, int x, int y);

        /** The button listing every tab, shown while not all tabs fit. */
        void openTabList(int x, int y);
    }

    protected TabStrip(int width, int height) {
        super(0, 0, width, height, Component.translatable("serverlistplus.tabs"));
    }

    /** Whether the tabs are stacked top to bottom. */
    public abstract boolean vertical();

    public abstract void setTabs(List<Tab> tabs, String selected);

    public abstract List<Tab> tabs();

    /** Highlights a tab while a server is dragged over it; null clears the highlight. */
    public abstract void setDropTarget(String id, boolean valid);

    /**
     * Turns a tab into an input field: its label is hidden so a text field can be placed on top.
     *
     * @param textWidth room the text needs; a row grows with it, a column keeps its width
     */
    public abstract void setEditing(String id, int textWidth);

    /** Where the label of a tab starts, relative to the tab. */
    public abstract int textInset(String id);

    /** Room for a label being edited in place, given the width the text needs. */
    public abstract int editorWidth(String id, int textWidth);

    /** Visible tabs and buttons with their positions. */
    public abstract List<Slot> slots();

    public Optional<Slot> slot(String id) {
        return slots().stream().filter(slot -> slot.id().equals(id)).findFirst();
    }

    /** The tab under the pointer (or {@link #NEW_TAB}), null for anything else. */
    public abstract String tabAt(double x, double y);

    public abstract void scrollIntoView(String id);
}
