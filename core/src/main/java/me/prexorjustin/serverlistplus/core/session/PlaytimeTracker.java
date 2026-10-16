package me.prexorjustin.serverlistplus.core.session;

import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.model.Store;

import java.util.function.LongSupplier;

/**
 * Counts time spent on servers and remembers when each was last played.
 * <p>
 * Playtime is measured with a monotonic clock (immune to system clock changes) and added to the
 * store in steps, so a crash loses at most {@link #FLUSH_INTERVAL_MS} of playtime.
 */
public final class PlaytimeTracker {
    public static final long FLUSH_INTERVAL_MS = 60_000;

    private final Store store;
    private final LongSupplier monotonicMillis;
    private final LongSupplier wallMillis;
    private String server;
    private long counted;

    /**
     * @param monotonicMillis a clock that only moves forward, for measuring durations
     * @param wallMillis      the current date as epoch millis, for "last played"
     */
    public PlaytimeTracker(Store store, LongSupplier monotonicMillis, LongSupplier wallMillis) {
        this.store = store;
        this.monotonicMillis = monotonicMillis;
        this.wallMillis = wallMillis;
    }

    public String currentServer() {
        return server;
    }

    /** Starts counting for a server; a previous session is ended first. */
    public void join(String key) {
        leave();
        server = key;
        counted = monotonicMillis.getAsLong();
        store.activityOrCreate(key).setLastPlayed(wallMillis.getAsLong());
    }

    /**
     * Call regularly while playing.
     *
     * @return true if the store changed and should be saved
     */
    public boolean tick() {
        if (server == null || monotonicMillis.getAsLong() - counted < FLUSH_INTERVAL_MS) return false;
        flush();
        return true;
    }

    /** @return true if the store changed and should be saved */
    public boolean leave() {
        if (server == null) return false;
        flush();
        server = null;
        return true;
    }

    private void flush() {
        long now = monotonicMillis.getAsLong();
        Activity activity = store.activityOrCreate(server);
        activity.addPlaytime(now - counted);
        activity.setLastPlayed(wallMillis.getAsLong());
        counted = now;
    }
}
