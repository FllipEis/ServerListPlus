package me.prexorjustin.serverlistplus.core.storage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SaveSchedulerTest {
    @Test
    void savesOnceAfterDelay() {
        AtomicInteger saves = new AtomicInteger();
        SaveScheduler scheduler = new SaveScheduler(1000, saves::incrementAndGet);

        scheduler.markDirty(0);
        scheduler.markDirty(500);
        scheduler.tick(999);
        assertEquals(0, saves.get());

        scheduler.tick(1000);
        assertEquals(1, saves.get());
        assertFalse(scheduler.isDirty());

        scheduler.tick(5000);
        assertEquals(1, saves.get());
    }

    @Test
    void flushSavesOnlyWhenDirty() {
        AtomicInteger saves = new AtomicInteger();
        SaveScheduler scheduler = new SaveScheduler(1000, saves::incrementAndGet);

        scheduler.flush();
        assertEquals(0, saves.get());

        scheduler.markDirty(0);
        scheduler.flush();
        assertEquals(1, saves.get());
    }
}
