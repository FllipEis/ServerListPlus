package me.prexorjustin.serverlistplus.ui.text;

import me.prexorjustin.serverlistplus.core.model.Activity;
import me.prexorjustin.serverlistplus.core.session.TimeFormat;
import me.prexorjustin.serverlistplus.core.style.Palette;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Short, translated texts about a server's history.
 */
public final class InfoText {
    private static final long MINUTE = 60_000;

    private InfoText() {
    }

    /** "2 d ago", or "just now". */
    public static Component ago(long lastPlayed) {
        TimeFormat.Amount amount = TimeFormat.ago(System.currentTimeMillis() - lastPlayed);
        if (amount == null) return Component.translatable("serverlistplus.time.now");
        return Component.translatable("serverlistplus.time.ago", unit(amount));
    }

    private static Component unit(TimeFormat.Amount amount) {
        return Component.translatable("serverlistplus.time.unit." + amount.unit().name().toLowerCase(Locale.ROOT), amount.value());
    }

    /** "14 h 3 min" or "25 min". */
    public static Component playtime(long millis) {
        long[] time = TimeFormat.hoursAndMinutes(millis);
        if (time[0] > 0) return Component.translatable("serverlistplus.time.hours_minutes", time[0], time[1]);
        return Component.translatable("serverlistplus.time.unit.minutes", time[1]);
    }

    /** "2 d ago · 14 h 3 min" for the server card; null if there is nothing to show. */
    public static Component summary(Activity activity) {
        if (activity == null) return null;
        Component last = lastPlayed(activity);
        Component playtime = playtimeOrNull(activity);
        if (last == null || playtime == null) return last != null ? last : playtime;
        return Component.empty().append(last).append(" · ").append(playtime);
    }

    /** "2 d ago", or null if the server was never played. */
    public static Component lastPlayed(Activity activity) {
        return activity.lastPlayed() > 0 ? ago(activity.lastPlayed()) : null;
    }

    /** "14 h 3 min", or null for less than a minute. */
    public static Component playtimeOrNull(Activity activity) {
        return activity.playtimeMs() >= MINUTE ? playtime(activity.playtimeMs()) : null;
    }

    /** Each tag gets a stable color from the palette, so the same tag looks the same everywhere. */
    public static int tagColor(String tag) {
        return Palette.forIndex(tag.toLowerCase(Locale.ROOT).hashCode()).argb(0x70);
    }
}
