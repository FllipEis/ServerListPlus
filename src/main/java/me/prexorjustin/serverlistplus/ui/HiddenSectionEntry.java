package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The header of the servers hidden from "All", at the end of that tab. Clicking it (handled by
 * the server list UI) folds the hidden servers in or out. Extends the LAN entry for the same
 * reason as {@link DividerEntry}.
 */
public final class HiddenSectionEntry extends ServerSelectionList.NetworkServerEntry {
    private static final LanServer IDENTITY = new LanServer("", "");
    private static final int TEXT = 0xFF9C9CA6;
    private static final int TEXT_HOVER = 0xFFEDEDF0;
    private static final int LINE = 0x40FFFFFF;
    private static final int HOVER_BG = 0x18FFFFFF;

    private final int count;
    private final boolean expanded;

    public HiddenSectionEntry(JoinMultiplayerScreen screen, int count, boolean expanded) {
        super(screen, IDENTITY);
        this.count = count;
        this.expanded = expanded;
    }

    @Override
    public Component getNarration() {
        return label();
    }

    private Component label() {
        return Component.literal(expanded ? "▾ " : "▸ ").append(Component.translatable("serverlistplus.hidden.section", count));
    }

    private void draw(Gfx gfx, int left, int top, int width, int height, boolean hovered) {
        Font font = Minecraft.getInstance().font;
        int middle = top + height / 2;
        if (hovered) gfx.roundedRect(left, top + 2, left + width, top + height - 2, 3, HOVER_BG);
        Component label = label();
        gfx.text(font, label, left + 6, middle - 4, hovered ? TEXT_HOVER : TEXT);
        gfx.fill(left + 6 + font.width(label) + 6, middle, left + width - 6, middle + 1, LINE);
    }

    //? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        draw(new Gfx(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), hovered);
    }
    //?} elif >=1.21.9 {
    /*@Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        draw(new Gfx(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), hovered);
    }
    *///?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height,
                       int mouseX, int mouseY, boolean hovered, float partialTick) {
        draw(new Gfx(graphics), left, top, width - 4, height, hovered);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    @Override
    public void join() {
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
    *///?}
}
