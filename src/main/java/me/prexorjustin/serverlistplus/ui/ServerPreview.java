package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.Pings;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Ids;
import me.prexorjustin.serverlistplus.compat.Textures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A server row like in the server list, for the address typed into Direct Connection:
 * icon, MOTD, player count and ping, filled in as the ping answers.
 */
public final class ServerPreview extends SlpWidget {
    public static final int HEIGHT = 36;
    private static final long GIVE_UP_MILLIS = 10_000;
    private static final int ICON = 32;
    private static final int CARD = 0x48000000;
    private static final int ICON_PLACEHOLDER = 0x30FFFFFF;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int MOTD_COLOR = 0xFF808080;
    private static final int DIM = 0xFF707070;

    private static int nextIconId;

    private final Font font;
    private Pings.Ping ping;
    private byte[] iconBytes;
    private Identifier icon;

    public ServerPreview(Font font) {
        super(0, 0, 0, HEIGHT, Component.translatable("serverlistplus.direct.preview"));
        this.font = font;
    }

    /** Shows the server being pinged, or nothing for null. */
    public void show(Pings.Ping ping) {
        this.ping = ping;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY) {
        if (ping == null) return;
        ServerData data = ping.data();
        int x = this.getX();
        int y = this.getY();
        gfx.roundedRect(x, y, this.getRight(), this.getBottom(), CARD);

        updateIcon(data.getIconBytes());
        if (icon != null) gfx.image(icon, x + 2, y + 2, ICON, ICON, 0, 0, 64, 64, 64, 64, 0xFFFFFFFF);
        else gfx.fill(x + 2, y + 2, x + 2 + ICON, y + 2 + ICON, ICON_PLACEHOLDER);

        int textX = x + 2 + ICON + 4;
        int right = this.getRight() - 4;
        Component status = status(data);
        int statusX = right - font.width(status);
        gfx.text(font, status, statusX, y + 3, DIM);

        String name = data.ip;
        int room = statusX - 6 - textX;
        if (font.width(name) > room) name = font.plainSubstrByWidth(name, Math.max(0, room - font.width("…"))) + "…";
        gfx.text(font, Component.literal(name), textX, y + 3, NAME_COLOR);

        List<FormattedCharSequence> lines = data.motd == null ? List.of() : font.split(data.motd, right - textX);
        for (int i = 0; i < Math.min(2, lines.size()); i++) gfx.text(font, lines.get(i), textX, y + 14 + 9 * i, MOTD_COLOR);
    }

    /** Players and ping once the server answered, the game version if it is incompatible. */
    private Component status(ServerData data) {
        if (ping.answered()) {
            if (!Pings.isCompatible(data)) return data.version.copy().withStyle(ChatFormatting.RED);
            ChatFormatting color = data.ping < 150 ? ChatFormatting.GREEN : data.ping < 300 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            return Component.empty().append(data.status == null ? Component.empty() : data.status).append("  ")
                .append(Component.translatable("serverlistplus.direct.ping", data.ping).withStyle(color));
        }
        if (data.state() == ServerData.State.INCOMPATIBLE) return data.version.copy().withStyle(ChatFormatting.RED);
        if (ping.failed() || ping.ageMillis() > GIVE_UP_MILLIS) return Component.literal("✕").withStyle(ChatFormatting.DARK_RED);
        int dots = (int) (ping.ageMillis() / 300 % 3);
        return Component.literal("●".repeat(dots + 1) + "○".repeat(2 - dots));
    }

    private void updateIcon(byte[] bytes) {
        if (Arrays.equals(bytes, iconBytes)) return;
        iconBytes = bytes;
        releaseTexture();
        if (bytes == null) return;
        try {
            Identifier id = Ids.of("direct/preview_" + nextIconId++);
            Textures.register(id, bytes);
            icon = id;
        } catch (IOException | RuntimeException e) {
            ServerListPlus.LOGGER.warn("Could not load the icon of {}", ping.data().ip, e);
        }
    }

    private void releaseTexture() {
        if (icon != null) Textures.release(icon);
        icon = null;
    }

    /** Frees the icon texture; it is loaded again if the preview is drawn later. */
    public void releaseIcon() {
        releaseTexture();
        iconBytes = null;
    }
}
