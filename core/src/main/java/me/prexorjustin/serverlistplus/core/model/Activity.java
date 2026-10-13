package me.prexorjustin.serverlistplus.core.model;

/**
 * When and how long a server was played. Kept per address, so every list entry pointing to the
 * same server shares it, while folders, pins, tags and notes belong to each entry.
 */
public final class Activity {
    /** Epoch millis of the last join, 0 if never joined. */
    private long lastPlayed;
    private long playtimeMs;

    public long lastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public long playtimeMs() {
        return playtimeMs;
    }

    public void addPlaytime(long millis) {
        if (millis > 0) playtimeMs += millis;
    }
}
