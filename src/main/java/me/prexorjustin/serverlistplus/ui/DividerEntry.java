package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.core.model.Divider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A separator line between servers, with an optional title in the middle.
 * <p>
 * Extends the LAN entry because {@code Entry#matches} is package-private on some versions,
 * so a direct subclass of {@code Entry} cannot be created from outside Minecraft's package.
 * A fixed {@link LanServer} per divider keeps the identity stable when the list is rebuilt.
 */
public final class DividerEntry extends ServerSelectionList.NetworkServerEntry {
    private static final Map<String, LanServer> IDENTITIES = new HashMap<>();
    private static final int LABEL_GAP = 6;

    private final Divider divider;
    private final int color;

    /** @param color ARGB color of line and title */
    public DividerEntry(JoinMultiplayerScreen screen, Divider divider, int color) {
        super(screen, IDENTITIES.computeIfAbsent(divider.id(), id -> new LanServer("", "")));
        this.divider = divider;
        this.color = color;
    }

    public Divider divider() {
        return divider;
    }

    @Override
    public Component getNarration() {
        return Component.literal(divider.label());
    }

    private void draw(Gfx gfx, int left, int top, int width, int height) {
        Font font = Minecraft.getInstance().font;
        int middle = top + height / 2;
        int right = left + width;
        int lineColor = color & 0x00FFFFFF | 0x90000000;
        if (divider.label().isEmpty()) {
            gfx.fill(left + 4, middle, right - 4, middle + 1, lineColor);
            return;
        }
        Component label = Component.literal(divider.label());
        int labelWidth = font.width(label);
        int labelX = left + (width - labelWidth) / 2;
        gfx.fill(left + 4, middle, labelX - LABEL_GAP, middle + 1, lineColor);
        gfx.fill(labelX + labelWidth + LABEL_GAP, middle, right - 4, middle + 1, lineColor);
        gfx.text(font, label, labelX, middle - 4, color | 0xFF000000);
    }

    //? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        draw(new Gfx(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
    }
    //?} elif >=1.21.9 {
    /*@Override
    public void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        draw(new Gfx(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
    }
    *///?} else {
    /*@Override
    public void render(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height,
                       int mouseX, int mouseY, boolean hovered, float partialTick) {
        draw(new Gfx(graphics), left, top, width - 4, height);
    }
    *///?}

    // Dividers are never selected; dragging and the menu are handled by the server list UI
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
