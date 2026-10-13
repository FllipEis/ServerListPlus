package me.prexorjustin.serverlistplus.core.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeFormatTest {
    private static final long MINUTE = 60_000;

    @Test
    void ago() {
        assertNull(TimeFormat.ago(30_000));
        assertEquals(new TimeFormat.Amount(5, TimeFormat.Unit.MINUTES), TimeFormat.ago(5 * MINUTE));
        assertEquals(new TimeFormat.Amount(2, TimeFormat.Unit.HOURS), TimeFormat.ago(150 * MINUTE));
        assertEquals(new TimeFormat.Amount(3, TimeFormat.Unit.DAYS), TimeFormat.ago(3 * 24 * 60 * MINUTE + 5));
        assertEquals(new TimeFormat.Amount(2, TimeFormat.Unit.WEEKS), TimeFormat.ago(15 * 24 * 60 * MINUTE));
        assertEquals(new TimeFormat.Amount(4, TimeFormat.Unit.MONTHS), TimeFormat.ago(125 * 24 * 60 * MINUTE));
        assertEquals(new TimeFormat.Amount(1, TimeFormat.Unit.YEARS), TimeFormat.ago(400 * 24 * 60 * MINUTE));
    }

    @Test
    void hoursAndMinutes() {
        assertArrayEquals(new long[]{14, 3}, TimeFormat.hoursAndMinutes((14 * 60 + 3) * MINUTE + 59_000));
        assertArrayEquals(new long[]{0, 0}, TimeFormat.hoursAndMinutes(-5));
    }
}
