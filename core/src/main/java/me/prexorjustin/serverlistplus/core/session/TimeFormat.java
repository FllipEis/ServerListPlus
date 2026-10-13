package me.prexorjustin.serverlistplus.core.session;

/**
 * Turns durations into the largest sensible unit, for short labels like "2 d" or "14 h 3 min".
 * The text itself is translated by the mod.
 */
public final class TimeFormat {
    public enum Unit { MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }

    /** An amount in a unit, e.g. 3 DAYS. */
    public record Amount(long value, Unit unit) {
    }

    private static final long MINUTE = 60_000;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    private TimeFormat() {
    }

    /** How long ago something happened, rounded down; null for less than a minute. */
    public static Amount ago(long millis) {
        if (millis < MINUTE) return null;
        if (millis < HOUR) return new Amount(millis / MINUTE, Unit.MINUTES);
        if (millis < DAY) return new Amount(millis / HOUR, Unit.HOURS);
        if (millis < WEEK) return new Amount(millis / DAY, Unit.DAYS);
        if (millis < MONTH) return new Amount(millis / WEEK, Unit.WEEKS);
        if (millis < YEAR) return new Amount(millis / MONTH, Unit.MONTHS);
        return new Amount(millis / YEAR, Unit.YEARS);
    }

    /** Hours and remaining minutes of a playtime; {@code {hours, minutes}}. */
    public static long[] hoursAndMinutes(long millis) {
        long minutes = Math.max(0, millis) / MINUTE;
        return new long[]{minutes / 60, minutes % 60};
    }
}
