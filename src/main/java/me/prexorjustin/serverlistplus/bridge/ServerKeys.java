package me.prexorjustin.serverlistplus.bridge;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.core.key.ServerKey;
import me.prexorjustin.serverlistplus.core.model.Store;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link ServerKey} of every saved server, by object.
 * <p>
 * {@code servers.dat} has no ids, so a server is recognised by address, name and its position
 * among identical entries. Remembering which object had which key lets data follow a server
 * when that changes: renamed, given a new address, or moved up because an identical entry
 * before it was deleted. This has to be global because vanilla hands the same
 * {@link ServerData} objects to every place that needs a key.
 */
public final class ServerKeys {
    private static final Map<ServerData, String> KEYS = new IdentityHashMap<>();

    private ServerKeys() {
    }

    /**
     * Works out the key of every saved server and keeps the stored data in line with them:
     * data follows servers whose key changed, data from before names were part of keys goes to
     * the matching servers, and data of deleted servers is cleaned up after a while.
     */
    public static void index(ServerList servers) {
        Map<ServerData, String> fresh = new IdentityHashMap<>();
        Map<String, Integer> seen = new HashMap<>();
        List<String> ordered = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            ServerData data = servers.get(i);
            int occurrence = seen.merge(ServerKey.of(data.ip, data.name), 1, Integer::sum) - 1;
            String key = ServerKey.of(data.ip, data.name, occurrence);
            fresh.put(data, key);
            ordered.add(key);
        }
        Map<String, String> moves = new LinkedHashMap<>();
        fresh.forEach((data, key) -> {
            String previous = KEYS.get(data);
            if (previous != null && !previous.equals(key)) moves.put(previous, key);
        });
        KEYS.clear();
        KEYS.putAll(fresh);

        Store store = ServerListPlus.store();
        long now = System.currentTimeMillis();
        boolean changed = !moves.isEmpty();
        if (changed) store.moveKeys(moves);
        changed |= store.adoptLegacyKeys(ordered);
        // An empty list is more likely a servers.dat that failed to load than a deliberate
        // deletion of every server, so nothing is marked or deleted then
        if (!ordered.isEmpty()) {
            changed |= store.markOrphans(Set.copyOf(ordered), now);
            changed |= store.purgeOrphans(now) > 0;
        }
        if (changed) ServerListPlus.save();
    }

    /** Key of a saved server; for any other server data, the key it gets once added at the end. */
    public static String of(ServerData data) {
        String key = KEYS.get(data);
        return key != null ? key : ifAdded(data);
    }

    private static String ifAdded(ServerData data) {
        String base = ServerKey.of(data.ip, data.name);
        int occurrence = 0;
        for (ServerData saved : KEYS.keySet()) {
            if (ServerKey.of(saved.ip, saved.name).equals(base)) occurrence++;
        }
        return ServerKey.of(data.ip, data.name, occurrence);
    }
}
