package me.prexorjustin.serverlistplus.core.style;

/**
 * What is drawn behind the server list while a folder is open.
 * Stored as text: {@code none}, {@code gradient}, {@code icon} or {@code image:<file name>}.
 */
public record Wallpaper(Kind kind, String file) {
    public enum Kind {
        NONE,
        /** A soft gradient in the folder color. */
        GRADIENT,
        /** The blurred icon of a server in the folder. */
        SERVER_ICON,
        /** An image from the wallpaper directory. */
        IMAGE
    }

    public static final Wallpaper NONE = new Wallpaper(Kind.NONE, null);
    public static final Wallpaper GRADIENT = new Wallpaper(Kind.GRADIENT, null);
    public static final Wallpaper SERVER_ICON = new Wallpaper(Kind.SERVER_ICON, null);

    private static final String IMAGE_PREFIX = "image:";

    public static Wallpaper image(String file) {
        return new Wallpaper(Kind.IMAGE, file);
    }

    /** Unknown or missing values fall back to the gradient. */
    public static Wallpaper parse(String value) {
        if (value == null) return GRADIENT;
        if (value.startsWith(IMAGE_PREFIX) && value.length() > IMAGE_PREFIX.length()) {
            return image(value.substring(IMAGE_PREFIX.length()));
        }
        return switch (value) {
            case "none" -> NONE;
            case "icon" -> SERVER_ICON;
            default -> GRADIENT;
        };
    }

    public String serialize() {
        return switch (kind) {
            case NONE -> "none";
            case GRADIENT -> "gradient";
            case SERVER_ICON -> "icon";
            case IMAGE -> IMAGE_PREFIX + file;
        };
    }
}
