package me.prexorjustin.serverlistplus.core.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.prexorjustin.serverlistplus.core.key.ServerKey;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.core.model.Store;

/**
 * Upgrades older file layouts step by step to {@link Store#CURRENT_VERSION}.
 * Add a case for every format change; never edit an existing step.
 */
final class Migrations {
    private Migrations() {
    }

    static JsonObject upgrade(JsonObject root, int fromVersion) {
        int version = fromVersion;
        while (version < Store.CURRENT_VERSION) {
            switch (version) {
                case 0 -> {
                    // Files without a version field predate versioning and already match v1
                }
                case 1 -> pinPerTab(root);
                case 2 -> severalFolders(root);
                case 3 -> activityPerAddress(root);
                default -> throw new IllegalStateException("No migration from version " + version);
            }
            version++;
        }
        if (fromVersion < Store.CURRENT_VERSION) root.addProperty("version", Store.CURRENT_VERSION);
        return root;
    }

    /**
     * v3 to v4: playtime and last join move out of the entries into one record per address.
     * Entries of one address hold copies of the same sessions, so the larger values are kept.
     */
    private static void activityPerAddress(JsonObject root) {
        if (!(root.get("servers") instanceof JsonObject servers)) return;
        JsonObject activity = new JsonObject();
        for (var entry : servers.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject server)) continue;
            long lastPlayed = asLong(server.remove("lastPlayed"));
            long playtime = asLong(server.remove("playtimeMs"));
            if (lastPlayed <= 0 && playtime <= 0) continue;
            String address = ServerKey.address(entry.getKey());
            JsonObject record = activity.get(address) instanceof JsonObject existing ? existing : new JsonObject();
            record.addProperty("lastPlayed", Math.max(lastPlayed, asLong(record.get("lastPlayed"))));
            record.addProperty("playtimeMs", Math.max(playtime, asLong(record.get("playtimeMs"))));
            activity.add(address, record);
        }
        root.add("activity", activity);
    }

    private static long asLong(JsonElement element) {
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsLong() : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** v2 to v3: a server can be in several folders. */
    private static void severalFolders(JsonObject root) {
        if (!(root.get("servers") instanceof JsonObject servers)) return;
        for (var entry : servers.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject server)) continue;
            JsonElement folder = server.remove("folderId");
            if (folder == null || !folder.isJsonPrimitive()) continue;
            JsonArray folders = new JsonArray();
            folders.add(folder.getAsString());
            server.add("folderIds", folders);
        }
    }

    /**
     * v1 to v2: pinning was global and there was a favorites tab. A pinned server now stays
     * on top in "All" and in its own folder, which is where it showed up first before.
     */
    private static void pinPerTab(JsonObject root) {
        if (root.get("servers") instanceof JsonObject servers) {
            for (var entry : servers.entrySet()) {
                if (!(entry.getValue() instanceof JsonObject server)) continue;
                JsonElement pinned = server.remove("pinned");
                if (pinned == null || !pinned.isJsonPrimitive() || !pinned.getAsBoolean()) continue;
                JsonArray tabs = new JsonArray();
                tabs.add(ViewSettings.TAB_ALL);
                if (server.get("folderId") instanceof JsonElement folder && folder.isJsonPrimitive()) tabs.add(folder.getAsString());
                server.add("pinnedTabs", tabs);
            }
        }
        if (root.get("view") instanceof JsonObject view) {
            view.remove("favoritesHidden");
            view.remove("favoritesStyle");
        }
    }
}
