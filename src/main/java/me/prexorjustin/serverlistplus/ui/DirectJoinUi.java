package me.prexorjustin.serverlistplus.ui;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.Pings;
import me.prexorjustin.serverlistplus.core.key.AddressKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Additions to the Direct Connection screen: a live preview of the typed server and the
 * addresses joined last. Lives as long as the screen, across resizes.
 */
public final class DirectJoinUi implements DirectHistory.Listener {
    private static final long PING_DELAY_MILLIS = 400;
    private static final int TOP = 36;
    private static final int GAP = 6;
    private static final int MIN_WIDTH = 200;
    private static final int MAX_WIDTH = 320;

    private final Screen parent;
    private final Consumer<String> setAddress;
    private final Runnable join;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final ServerPreview preview;
    private final DirectHistory history;

    private String pinged;
    private long pingAt;

    /**
     * @param parent     the server list the screen was opened from
     * @param setAddress puts an address into the input field
     * @param join       joins the address in the input field
     */
    public DirectJoinUi(Font font, Screen parent, Consumer<String> setAddress, Runnable join) {
        this.parent = parent;
        this.setAddress = setAddress;
        this.join = join;
        this.preview = new ServerPreview(font);
        this.history = new DirectHistory(font, this, this::savedName);
        history.setAddresses(ServerListPlus.store().recentDirect());
    }

    /**
     * Arranges the screen top to bottom: label and input, preview, history, and the two buttons
     * side by side at the bottom.
     *
     * @return the widgets to add to the screen
     */
    public List<AbstractWidget> layout(int width, int height, EditBox input, Button select, Button cancel) {
        int columnWidth = Math.clamp(width - 40, MIN_WIDTH, MAX_WIDTH);
        int x = (width - columnWidth) / 2;
        input.setRectangle(columnWidth, 20, x, TOP + 16);
        preview.setRectangle(columnWidth, ServerPreview.HEIGHT, x, input.getBottom() + GAP);

        int buttonY = height - 28;
        int buttonWidth = (columnWidth - 4) / 2;
        select.setRectangle(buttonWidth, 20, x, buttonY);
        if (cancel != null) cancel.setRectangle(columnWidth - buttonWidth - 4, 20, x + buttonWidth + 4, buttonY);

        int historyY = preview.getBottom() + GAP + 2;
        history.setRectangle(columnWidth, Math.max(0, buttonY - GAP - historyY), x, historyY);
        return List.of(preview, history);
    }

    /** Where vanilla draws the "Server Address" label. */
    public static int labelY() {
        return TOP + 4;
    }

    /** Pings the new address shortly after typing stops. */
    public void addressChanged(String address) {
        if (address.trim().equals(pinged)) {
            pingAt = 0;
            return;
        }
        pingAt = System.currentTimeMillis() + (pinged == null ? 0 : PING_DELAY_MILLIS);
    }

    public void tick(String address) {
        pinger.tick();
        if (pingAt != 0 && System.currentTimeMillis() >= pingAt) {
            pingAt = 0;
            ping(address.trim());
        }
    }

    private void ping(String address) {
        pinged = address;
        pinger.removeAll();
        if (!ServerAddress.isValidAddress(address)) {
            preview.show(null);
            return;
        }
        ServerData data = new ServerData("", address, ServerData.Type.OTHER);
        // The pinger fills these in from another thread; until then they must not be null
        data.motd = Component.empty();
        data.status = Component.empty();
        preview.show(Pings.start(pinger, data));
    }

    /** Remembers the address that is about to be joined. */
    public void joining(String address) {
        ServerListPlus.store().rememberDirect(address);
        ServerListPlus.save();
    }

    public void close() {
        pinger.removeAll();
        preview.releaseIcon();
    }

    private ServerList servers() {
        return parent instanceof MultiplayerScreenHook hook ? hook.serverlistplus$servers() : null;
    }

    private String savedName(String address) {
        ServerList servers = servers();
        if (servers == null) return null;
        String key = AddressKey.normalize(address);
        for (int i = 0; i < servers.size(); i++) {
            if (AddressKey.normalize(servers.get(i).ip).equals(key)) return NameText.plain(servers.get(i).name);
        }
        return null;
    }

    // ---- DirectHistory.Listener ----

    @Override
    public void pick(String address) {
        setAddress.accept(address);
    }

    @Override
    public void join(String address) {
        setAddress.accept(address);
        join.run();
    }

    @Override
    public void remove(String address) {
        ServerListPlus.store().forgetDirect(address);
        ServerListPlus.save();
        history.setAddresses(ServerListPlus.store().recentDirect());
    }

    @Override
    public void addToList(String address) {
        ServerList servers = servers();
        if (servers == null) return;
        servers.add(new ServerData(address, address, ServerData.Type.OTHER), false);
        servers.save();
        ((MultiplayerScreenHook) parent).serverlistplus$refresh();
    }
}
