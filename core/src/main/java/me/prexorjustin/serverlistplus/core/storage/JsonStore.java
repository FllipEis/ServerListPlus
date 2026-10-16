package me.prexorjustin.serverlistplus.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import me.prexorjustin.serverlistplus.core.model.Store;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes a {@link Store} as JSON without ever leaving a half-written file behind.
 */
public final class JsonStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;

    public JsonStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public enum Status {
        LOADED,
        /** No file yet, started empty. */
        MISSING,
        /** File was unreadable; it was copied to {@code .bak} and an empty store is used. */
        CORRUPT,
        /** File was written by a newer mod version; loaded as-is but must not be saved. */
        NEWER_VERSION
    }

    public record LoadResult(Store store, Status status) {
        public boolean writable() {
            return status != Status.NEWER_VERSION;
        }
    }

    public LoadResult load() throws IOException {
        if (Files.notExists(file)) return new LoadResult(new Store(), Status.MISSING);

        Store store;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) throw new JsonParseException("Root is not an object");
            JsonObject object = root.getAsJsonObject();
            int version = object.has("version") ? object.get("version").getAsInt() : 0;
            store = GSON.fromJson(Migrations.upgrade(object, version), Store.class);
            if (store == null) throw new JsonParseException("Empty document");
            store.sanitize();
            if (version > Store.CURRENT_VERSION) return new LoadResult(store, Status.NEWER_VERSION);
        } catch (RuntimeException e) {
            // Any malformed content (bad JSON, wrong types, unknown migration) counts as corrupt
            Files.copy(file, backupFile(), StandardCopyOption.REPLACE_EXISTING);
            return new LoadResult(new Store(), Status.CORRUPT);
        }
        return new LoadResult(store, Status.LOADED);
    }

    public void save(Store store) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(store, writer);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Path backupFile() {
        return file.resolveSibling(file.getFileName() + ".bak");
    }
}
