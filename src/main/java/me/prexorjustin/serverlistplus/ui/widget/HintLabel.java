package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * A line of gray help text, cut off with "…" when it does not fit.
 */
public final class HintLabel extends SlpWidget {
    private static final int COLOR = 0xFFB0B0B0;

    private final Font font;

    public HintLabel(Font font, Component text) {
        super(0, 0, 0, 20, text);
        this.font = font;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        String text = this.getMessage().getString();
        if (font.width(text) > this.getWidth()) {
            while (!text.isEmpty() && font.width(text + "…") > this.getWidth()) text = text.substring(0, text.length() - 1);
            text += "…";
        }
        gfx.text(font, Component.literal(text), this.getX(), this.getY() + (this.getHeight() - 8) / 2, COLOR);
    }
}
