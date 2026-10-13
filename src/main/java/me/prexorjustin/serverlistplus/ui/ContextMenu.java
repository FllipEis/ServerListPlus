package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A right-click menu with nested submenus that open on hover. Input is routed to it by
 * the server list UI, so it only draws itself as a widget.
 */
public final class ContextMenu extends SlpWidget {
    /**
     * @param checked shows a check mark column: true or false for choices, null for plain items
     */
    public record Item(Component label, Runnable action, List<Item> children, boolean enabled, Boolean checked) {
        public Item(Component label, Runnable action, List<Item> children, boolean enabled) {
            this(label, action, children, enabled, null);
        }

        public static Item action(Component label, Runnable action) {
            return new Item(label, action, List.of(), true);
        }

        public static Item title(Component label) {
            return new Item(label, null, List.of(), false);
        }

        public static Item submenu(Component label, List<Item> children) {
            return new Item(label, null, children, !children.isEmpty());
        }

        public static Item separator() {
            return new Item(null, null, List.of(), false);
        }

        /** Marks this item as a choice that is currently selected or not. */
        public Item checked(boolean checked) {
            return new Item(label, action, children, enabled, checked);
        }

        boolean isSeparator() {
            return label == null;
        }

        boolean isTitle() {
            return label != null && action == null && children.isEmpty();
        }

        boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    private static final int ITEM_HEIGHT = 16;
    private static final int SEPARATOR_HEIGHT = 7;
    /** Space between the panel edge and its items. */
    private static final int INSET = 4;
    private static final int PAD = 8;
    private static final int CHECK_SPACE = 12;
    private static final int ARROW_SPACE = 14;
    private static final int RADIUS = 4;
    private static final int SHADOW_NEAR = 0x50000000;
    private static final int SHADOW_FAR = 0x24000000;
    private static final int BORDER = 0xFF34343C;
    private static final int BACKGROUND = 0xFF1A1A1F;
    private static final int HOVER = 0xFF2E5FC7;
    private static final int TEXT = 0xFFEDEDF0;
    private static final int TEXT_DISABLED = 0xFF66666E;
    private static final int TITLE = 0xFF9C9CA6;
    private static final int CHECK = 0xFF6EA0FF;
    private static final int ARROW = 0xFF9C9CA6;
    private static final int SEPARATOR = 0xFF2C2C33;

    /** One open level of the menu; {@code openIndex} is the item whose submenu is shown next to it. */
    private static final class Panel {
        final List<Item> items;
        final boolean hasChecks;
        final int x;
        final int y;
        final int width;
        final int height;
        int openIndex = -1;

        Panel(List<Item> items, boolean hasChecks, int x, int y, int width, int height) {
            this.items = items;
            this.hasChecks = hasChecks;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }

        int indexAt(double py) {
            int top = y + INSET;
            for (int i = 0; i < items.size(); i++) {
                int itemHeight = items.get(i).isSeparator() ? SEPARATOR_HEIGHT : ITEM_HEIGHT;
                if (py >= top && py < top + itemHeight) return i;
                top += itemHeight;
            }
            return -1;
        }

        int itemTop(int index) {
            int top = y + INSET;
            for (int i = 0; i < index; i++) top += items.get(i).isSeparator() ? SEPARATOR_HEIGHT : ITEM_HEIGHT;
            return top;
        }
    }

    private final Font font;
    private final int screenWidth;
    private final int screenHeight;
    /** The root menu first, then each open submenu. */
    private final List<Panel> panels = new ArrayList<>();

    public ContextMenu(Font font, List<Item> items, int x, int y, int screenWidth, int screenHeight) {
        super(0, 0, 0, 0, Component.empty());
        this.font = font;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        panels.add(panel(items, x, y));
    }

    private Panel panel(List<Item> items, int x, int y) {
        boolean hasChecks = items.stream().anyMatch(item -> item.checked() != null);
        int width = 0;
        int height = 2 * INSET;
        for (Item item : items) {
            if (item.isSeparator()) {
                height += SEPARATOR_HEIGHT;
                continue;
            }
            width = Math.max(width, font.width(item.label()) + (item.hasChildren() ? ARROW_SPACE : 0));
            height += ITEM_HEIGHT;
        }
        width += 2 * INSET + 2 * PAD + (hasChecks ? CHECK_SPACE : 0);
        x = Math.max(2, Math.min(x, screenWidth - width - 4));
        y = Math.max(2, Math.min(y, screenHeight - height - 4));
        return new Panel(items, hasChecks, x, y, width, height);
    }

    /** The deepest open panel under the pointer, or -1. */
    private int panelAt(double x, double y) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).contains(x, y)) return i;
        }
        return -1;
    }

    public boolean contains(double x, double y) {
        return panelAt(x, y) >= 0;
    }

    /** Opens the submenu of the hovered item and closes deeper ones, so the menu follows the mouse. */
    private void hover(double x, double y) {
        int level = panelAt(x, y);
        if (level < 0) return;
        Panel panel = panels.get(level);
        int index = panel.indexAt(y);
        if (index < 0 || index == panel.openIndex) return;
        Item item = panel.items.get(index);
        if (item.hasChildren() && item.enabled()) {
            open(level, index);
        } else if (!item.isSeparator()) {
            closeAbove(level);
        }
    }

    private void closeAbove(int level) {
        while (panels.size() > level + 1) panels.removeLast();
        panels.get(level).openIndex = -1;
    }

    private void open(int level, int index) {
        closeAbove(level);
        Panel parent = panels.get(level);
        parent.openIndex = index;
        List<Item> children = parent.items.get(index).children();
        Panel sub = panel(children, parent.x + parent.width + 2, parent.itemTop(index) - INSET);
        if (sub.x < parent.x + parent.width + 2) {
            // Not enough room on the right, open to the left instead
            sub = panel(children, parent.x - sub.width - 2, sub.y);
        }
        panels.add(sub);
    }

    /**
     * Handles a click.
     *
     * @return true if the menu should close
     */
    public boolean click(double x, double y) {
        int level = panelAt(x, y);
        if (level < 0) return true;
        Panel panel = panels.get(level);
        int index = panel.indexAt(y);
        if (index < 0) return false;
        Item item = panel.items.get(index);
        if (!item.enabled()) return false;
        if (item.hasChildren()) {
            open(level, index);
            return false;
        }
        if (item.action() != null) item.action().run();
        return true;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        hover(mouseX, mouseY);
        for (Panel panel : panels) {
            gfx.overlay();
            drawPanel(gfx, panel, mouseX, mouseY);
        }
    }

    private void drawPanel(Gfx gfx, Panel panel, int mouseX, int mouseY) {
        int x1 = panel.x;
        int y1 = panel.y;
        int x2 = panel.x + panel.width;
        int y2 = panel.y + panel.height;
        // Soft shadow, then a thin border around the body
        gfx.roundedRect(x1 - 1, y1 + 1, x2 + 3, y2 + 4, RADIUS + 2, SHADOW_FAR);
        gfx.roundedRect(x1, y1 + 1, x2 + 1, y2 + 2, RADIUS + 1, SHADOW_NEAR);
        gfx.roundedRect(x1, y1, x2, y2, RADIUS, BORDER);
        gfx.roundedRect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, RADIUS - 1, BACKGROUND);

        int hovered = panel.contains(mouseX, mouseY) ? panel.indexAt(mouseY) : -1;
        int textX = x1 + INSET + PAD + (panel.hasChecks ? CHECK_SPACE : 0);
        for (int i = 0; i < panel.items.size(); i++) {
            Item item = panel.items.get(i);
            int top = panel.itemTop(i);
            if (item.isSeparator()) {
                gfx.fill(x1 + INSET + 4, top + SEPARATOR_HEIGHT / 2, x2 - INSET - 4, top + SEPARATOR_HEIGHT / 2 + 1, SEPARATOR);
                continue;
            }
            int textY = top + (ITEM_HEIGHT - 8) / 2;
            if (item.isTitle()) {
                gfx.text(font, item.label(), x1 + INSET + PAD, textY, TITLE);
                continue;
            }
            boolean highlighted = item.enabled() && (i == hovered || i == panel.openIndex);
            if (highlighted) gfx.roundedRect(x1 + INSET, top, x2 - INSET, top + ITEM_HEIGHT, 3, HOVER);
            if (Boolean.TRUE.equals(item.checked())) {
                gfx.text(font, Component.literal("✔"), x1 + INSET + PAD - 2, textY, highlighted ? TEXT : CHECK);
            }
            gfx.text(font, item.label(), textX, textY, item.enabled() ? TEXT : TEXT_DISABLED);
            if (item.hasChildren()) gfx.text(font, Component.literal("›"), x2 - INSET - PAD - 2, textY, highlighted ? TEXT : ARROW);
        }
    }
}
