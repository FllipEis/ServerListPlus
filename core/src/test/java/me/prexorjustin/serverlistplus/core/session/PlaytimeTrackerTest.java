package me.prexorjustin.serverlistplus.core.session;

import me.prexorjustin.serverlistplus.core.model.Store;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytimeTrackerTest {
    private final AtomicLong mono = new AtomicLong(1_000);
    private final AtomicLong wall = new AtomicLong(1_700_000_000_000L);
    private final Store store = new Store();
    private final PlaytimeTracker tracker = new PlaytimeTracker(store, mono::get, wall::get);

    private void advance(long millis) {
        mono.addAndGet(millis);
        wall.addAndGet(millis);
    }

    @Test
    void joinSetsLastPlayed() {
        tracker.join("a");
        assertEquals(wall.get(), store.activity("a").orElseThrow().lastPlayed());
        assertEquals("a", tracker.currentServer());
    }

    @Test
    void flushesEveryMinuteAndOnLeave() {
        tracker.join("a");
        advance(30_000);
        assertFalse(tracker.tick());
        advance(30_000);
        assertTrue(tracker.tick());
        assertEquals(60_000, store.activity("a").orElseThrow().playtimeMs());

        advance(15_000);
        assertTrue(tracker.leave());
        assertEquals(75_000, store.activity("a").orElseThrow().playtimeMs());
        assertEquals(wall.get(), store.activity("a").orElseThrow().lastPlayed());
        assertNull(tracker.currentServer());
    }

    @Test
    void wallClockJumpsDoNotChangePlaytime() {
        tracker.join("a");
        wall.addAndGet(-10 * 3_600_000L);
        advance(5_000);
        tracker.leave();
        assertEquals(5_000, store.activity("a").orElseThrow().playtimeMs());
    }

    @Test
    void joiningAnotherServerEndsTheFirstSession() {
        tracker.join("a");
        advance(10_000);
        tracker.join("b");
        advance(20_000);
        tracker.leave();
        assertEquals(10_000, store.activity("a").orElseThrow().playtimeMs());
        assertEquals(20_000, store.activity("b").orElseThrow().playtimeMs());
    }

    @Test
    void nothingToDoWithoutSession() {
        assertFalse(tracker.tick());
        assertFalse(tracker.leave());
    }
}
