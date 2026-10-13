package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Follows the mouse while a server is dragged, and marks where it would be inserted.
 */
public final class DragGhost extends SlpWidget {
    private static final int BACKGROUND = 0xE0101010;
    private static final int BORDER_OK = 0xFFFFFFFF;
    private static final int BORDER_BAD = 0xFFFF5555;
    private static final int LINE = 0xFFFFFFFF;

    private final Font font;
    private final Component label;
    private boolean valid = true;
    private int lineY = -1;
    private int lineLeft;
    private int lineRight;

    public DragGhost(Font font, Component label) {
        super(0, 0, 0, 0, label);
        this.font = font;
        this.label = label;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /** Shows the insertion line at {@code y}; a negative {@code y} hides it. */
    public void setLine(int y, int left, int right) {
        this.lineY = y;
        this.lineLeft = left;
        this.lineRight = right;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        gfx.overlay();
        if (lineY >= 0) gfx.fill(lineLeft, lineY - 1, lineRight, lineY + 1, LINE);
        int x = mouseX + 10;
        int y = mouseY + 6;
        int width = font.width(label) + 8;
        gfx.fill(x, y, x + width, y + 14, BACKGROUND);
        gfx.outline(x, y, x + width, y + 14, valid ? BORDER_OK : BORDER_BAD);
        gfx.text(font, label, x + 4, y + 3, 0xFFFFFFFF);
    }
}
