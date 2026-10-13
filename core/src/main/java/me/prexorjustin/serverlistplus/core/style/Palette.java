package me.prexorjustin.serverlistplus.core.style;

import java.util.List;
import java.util.Optional;

/**
 * The colors a folder can have. Chosen to stay readable on dark backgrounds.
 */
public final class Palette {
    public record Color(String key, int rgb) {
        public int argb(int alpha) {
            return (alpha & 0xFF) << 24 | rgb;
        }
    }

    public static final List<Color> COLORS = List.of(
        new Color("red", 0xE5484D),
        new Color("orange", 0xF76B15),
        new Color("amber", 0xFFB224),
        new Color("green", 0x46A758),
        new Color("teal", 0x12A594),
        new Color("cyan", 0x05A2C2),
        new Color("blue", 0x0090FF),
        new Color("indigo", 0x3E63DD),
        new Color("purple", 0x8E4EC6),
        new Color("pink", 0xD6409F),
        new Color("brown", 0xAD7F58),
        new Color("gray", 0x8B8D98)
    );

    private Palette() {
    }

    /** Colors for new folders take turns, so neighbouring tabs differ. */
    public static Color forIndex(int index) {
        return COLORS.get(Math.floorMod(index, COLORS.size()));
    }

    public static Optional<Color> byKey(String key) {
        return COLORS.stream().filter(color -> color.key().equals(key)).findFirst();
    }
}
