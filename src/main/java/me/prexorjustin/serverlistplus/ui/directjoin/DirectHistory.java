package me.prexorjustin.serverlistplus.ui.directjoin;

import me.prexorjustin.serverlistplus.ui.widget.SlpWidget;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

/**
 * The addresses last joined through Direct Connection. Click to use one, double click to join it;
 * hovering a row offers adding it to the server list and removing it from the history.
 */
public final class DirectHistory extends SlpWidget {
    public interface Listener {
        void pick(String address);

        void join(String address);

        void remove(String address);

        void addToList(String address);
    }

    private static final int HEADER = 14;
    private static final int ROW = 13;
    private static final int BUTTON = 12;
    private static final String REMOVE = "✕";
    private static final String ADD = "+";
    private static final int HEADER_COLOR = 0xFFA0A0A0;
    private static final int HINT_COLOR = 0xFF606060;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int NAME = 0xFF808080;
    private static final int HOVER_BG = 0x20FFFFFF;
    private static final int BUTTON_HOVER_BG = 0x30FFFFFF;

    private final Font font;
    private final Listener listener;
    /** Name of the saved server with that address, null if it is not in the server list. */
    private final Function<String, String> savedName;
    private List<String> addresses = List.of();

    public DirectHistory(Font font, Listener listener, Function<String, String> savedName) {
        super(0, 0, 0, 0, Component.translatable("serverlistplus.direct.recent"));
        this.font = font;
        this.listener = listener;
        this.savedName = savedName;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = List.copyOf(addresses);
    }

    private int visibleRows() {
        return Math.max(0, Math.min(addresses.size(), (this.getHeight() - HEADER) / ROW));
    }

    private int rowAt(double y) {
        int row = (int) Math.floor((y - this.getY() - HEADER) / ROW);
        return row >= 0 && row < visibleRows() ? row : -1;
    }

    private int removeX() {
        return this.getRight() - BUTTON;
    }

    private int addX() {
        return removeX() - BUTTON - 1;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        if (this.getHeight() < HEADER + ROW) return;
        int x = this.getX();
        gfx.text(font, this.getMessage(), x + 1, this.getY() + 2, HEADER_COLOR);
        Component hint = Component.translatable("serverlistplus.direct.hint");
        if (!addresses.isEmpty() && font.width(this.getMessage()) + font.width(hint) + 12 < this.getWidth()) {
            gfx.text(font, hint, this.getRight() - font.width(hint) - 1, this.getY() + 2, HINT_COLOR);
        }
        if (addresses.isEmpty()) {
            gfx.text(font, Component.translatable("serverlistplus.direct.none").withStyle(ChatFormatting.ITALIC), x + 4, this.getY() + HEADER + 2, HINT_COLOR);
            return;
        }

        int hovered = this.isMouseOver(mouseX, mouseY) ? rowAt(mouseY) : -1;
        for (int i = 0; i < visibleRows(); i++) {
            String address = addresses.get(i);
            int top = this.getY() + HEADER + i * ROW;
            int textY = top + (ROW - 8) / 2;
            boolean isHovered = i == hovered;
            if (isHovered) gfx.roundedRect(x, top, this.getRight(), top + ROW, HOVER_BG);

            String name = savedName.apply(address);
            int limit = (isHovered ? (name == null ? addX() : removeX()) : this.getRight()) - 4;
            gfx.text(font, Component.literal(fit(address, limit - x - 4)), x + 4, textY, TEXT);
            int afterAddress = x + 4 + font.width(address) + 6;
            if (name != null && afterAddress + font.width(name) < limit) gfx.text(font, Component.literal(name), afterAddress, textY, NAME);
            if (!isHovered) continue;

            drawButton(gfx, REMOVE, removeX(), top, mouseX, mouseY, Component.translatable("serverlistplus.direct.remove"));
            if (name == null) drawButton(gfx, ADD, addX(), top, mouseX, mouseY, Component.translatable("serverlistplus.direct.add"));
        }
    }

    private void drawButton(Gfx gfx, String symbol, int x, int top, int mouseX, int mouseY, Component tooltip) {
        boolean over = mouseX >= x && mouseX < x + BUTTON;
        if (over) {
            gfx.roundedRect(x, top, x + BUTTON, top + ROW, BUTTON_HOVER_BG);
            gfx.tooltip(font, tooltip, mouseX, mouseY);
        }
        gfx.text(font, Component.literal(symbol), x + (BUTTON - font.width(symbol)) / 2, top + (ROW - 8) / 2, over ? 0xFFFFFFFF : NAME);
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    @Override
    protected boolean press(Pointer pointer, boolean doubleClick) {
        int row = rowAt(pointer.y());
        if (row < 0 || !pointer.isLeft()) return false;
        String address = addresses.get(row);
        if (pointer.x() >= removeX()) {
            listener.remove(address);
        } else if (pointer.x() >= addX() && savedName.apply(address) == null) {
            listener.addToList(address);
        } else if (doubleClick) {
            listener.join(address);
        } else {
            listener.pick(address);
        }
        return true;
    }
}
