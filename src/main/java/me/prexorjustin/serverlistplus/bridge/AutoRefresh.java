package me.prexorjustin.serverlistplus.bridge;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Pings the shown servers again every few seconds, so MOTD, player count and ping stay current.
 * Each ping runs on a copy of the server, so the row keeps its old status until the new one
 * arrives instead of flashing "Pinging…".
 */
public final class AutoRefresh {
    private static final long TIMEOUT_MILLIS = 10_000;
    private static final Component CANT_CONNECT = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.DARK_RED);

    private final JoinMultiplayerScreen screen;
    private final Map<ServerSelectionList.OnlineServerEntry, Pings.Ping> pending = new HashMap<>();
    private long lastRefresh = System.currentTimeMillis();

    public AutoRefresh(JoinMultiplayerScreen screen) {
        this.screen = screen;
    }

    /** Call every tick. */
    public void tick(ServerSelectionList list, int intervalSeconds) {
        applyResults();
        long now = System.currentTimeMillis();
        if (intervalSeconds <= 0) {
            lastRefresh = now;
            return;
        }
        if (now - lastRefresh < intervalSeconds * 1000L) return;
        lastRefresh = now;
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof ServerSelectionList.OnlineServerEntry online) refresh(online);
        }
    }

    private void refresh(ServerSelectionList.OnlineServerEntry entry) {
        ServerData data = entry.getServerData();
        // Vanilla is still pinging this one for the first time
        if (data.state() == ServerData.State.INITIAL || data.state() == ServerData.State.PINGING) return;
        if (pending.containsKey(entry)) return;

        ServerData copy = new ServerData(data.name, data.ip, data.type());
        copy.setIconBytes(data.getIconBytes());
        copy.motd = data.motd;
        copy.status = data.status;
        pending.put(entry, Pings.start(screen.getPinger(), copy));
    }

    /** Copies finished pings into the shown servers, on the render thread. */
    private void applyResults() {
        Iterator<Map.Entry<ServerSelectionList.OnlineServerEntry, Pings.Ping>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerSelectionList.OnlineServerEntry, Pings.Ping> next = it.next();
            Pings.Ping ping = next.getValue();
            boolean timedOut = ping.ageMillis() > TIMEOUT_MILLIS;
            if (!ping.answered() && !ping.failed() && !timedOut) continue;
            it.remove();
            ServerData data = next.getKey().getServerData();
            if (ping.answered() || ping.data().state() == ServerData.State.INCOMPATIBLE) {
                apply(ping.data(), data);
            } else {
                data.setState(ServerData.State.UNREACHABLE);
                data.motd = CANT_CONNECT;
                data.status = Component.empty();
                data.playerList = List.of();
            }
            ((ServerEntryHook) next.getKey()).serverlistplus$refreshStatus();
        }
    }

    private void apply(ServerData from, ServerData to) {
        to.motd = from.motd;
        to.status = from.status;
        to.players = from.players;
        to.playerList = from.playerList;
        to.protocol = from.protocol;
        to.version = from.version;
        to.ping = from.ping;
        //? if neoforge
        /*to.neoForgeData = from.neoForgeData;*/
        to.setState(Pings.isCompatible(from) ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
        if (!Arrays.equals(from.getIconBytes(), to.getIconBytes())) {
            to.setIconBytes(from.getIconBytes());
            screen.getServers().save();
        }
    }
}
