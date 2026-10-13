package me.prexorjustin.serverlistplus.core.query;

import me.prexorjustin.serverlistplus.core.text.MiniText;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Text normalization for comparisons: no formatting codes or MiniMessage tags, lower case.
 */
public final class PlainText {
    private static final Pattern FORMATTING_CODE = Pattern.compile("§.?");

    private PlainText() {
    }

    public static String of(String text) {
        if (text == null) return "";
        return FORMATTING_CODE.matcher(MiniText.plain(text)).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
