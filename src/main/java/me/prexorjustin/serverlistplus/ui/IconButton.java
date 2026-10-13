package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A square button showing only a {@link Glyph}, styled like the tabs next to it.
 */
public final class IconButton extends SlpWidget {
    public static final int SIZE = 20;

    private static final int HOVER_BG = 0x18FFFFFF;
    private static final int HIGHLIGHT_BG = 0x30FFFFFF;
    private static final int COLOR = 0xFFA8A8A8;
    private static final int COLOR_HOVER = 0xFFFFFFFF;
    private static final int COLOR_HIGHLIGHT = 0xFFFFAA00;

    private final Glyph glyph;
    private final Consumer<IconButton> action;
    private final BooleanSupplier highlighted;

    /**
     * @param highlighted whether the button shows a non-default state, such as an active sort order
     */
    public IconButton(Glyph glyph, Component label, Consumer<IconButton> action, BooleanSupplier highlighted) {
        super(0, 0, SIZE, SIZE, label);
        this.glyph = glyph;
        this.action = action;
        this.highlighted = highlighted;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        boolean hovered = this.isMouseOver(mouseX, mouseY);
        boolean highlight = highlighted.getAsBoolean();
        if (highlight) gfx.roundedRect(this.getX(), this.getY(), this.getRight(), this.getBottom(), HIGHLIGHT_BG);
        else if (hovered) gfx.roundedRect(this.getX(), this.getY(), this.getRight(), this.getBottom(), HOVER_BG);
        int x = this.getX() + (this.getWidth() - glyph.width()) / 2;
        int y = this.getY() + (this.getHeight() - glyph.height()) / 2;
        glyph.draw(gfx, x, y, highlight ? COLOR_HIGHLIGHT : hovered ? COLOR_HOVER : COLOR);
    }

    @Override
    protected boolean press(Pointer pointer, boolean doubleClick) {
        if (!pointer.isLeft()) return false;
        action.accept(this);
        return true;
    }
}
