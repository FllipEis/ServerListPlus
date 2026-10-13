package me.prexorjustin.serverlistplus.core.storage;

/**
 * Coalesces many small changes into one write: a save runs once {@code delayMs}
 * have passed since the first unsaved change.
 */
public final class SaveScheduler {
    private final long delayMs;
    private final Runnable save;
    private long dirtySince = -1;

    public SaveScheduler(long delayMs, Runnable save) {
        this.delayMs = delayMs;
        this.save = save;
    }

    public void markDirty(long now) {
        if (dirtySince < 0) dirtySince = now;
    }

    public boolean isDirty() {
        return dirtySince >= 0;
    }

    /** Call regularly (e.g. every client tick). */
    public void tick(long now) {
        if (dirtySince >= 0 && now - dirtySince >= delayMs) flush();
    }

    /** Saves immediately if anything changed, e.g. when the game closes. */
    public void flush() {
        if (dirtySince < 0) return;
        dirtySince = -1;
        save.run();
    }
}
