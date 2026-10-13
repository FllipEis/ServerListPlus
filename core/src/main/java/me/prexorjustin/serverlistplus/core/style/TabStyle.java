package me.prexorjustin.serverlistplus.core.style;

/**
 * Look of the built-in "All" tab; folders store the same values themselves.
 */
public final class TabStyle implements Styled {
    private String color;
    private String icon;
    private String wallpaper;
    private WallpaperFrame wallpaperFrame;

    public static TabStyle all() {
        TabStyle style = new TabStyle();
        style.setWallpaper(Wallpaper.NONE);
        return style;
    }

    @Override
    public Palette.Color color() {
        return color == null ? null : Palette.byKey(color).orElse(null);
    }

    @Override
    public void setColor(Palette.Color color) {
        this.color = color == null ? null : color.key();
    }

    @Override
    public String icon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon == null || icon.isBlank() ? null : icon;
    }

    @Override
    public Wallpaper wallpaper() {
        return Wallpaper.parse(wallpaper);
    }

    @Override
    public void setWallpaper(Wallpaper wallpaper) {
        this.wallpaper = wallpaper == null ? null : wallpaper.serialize();
    }

    @Override
    public WallpaperFrame wallpaperFrame() {
        if (wallpaperFrame == null) wallpaperFrame = new WallpaperFrame();
        return wallpaperFrame;
    }

    @Override
    public void setWallpaperFrame(WallpaperFrame frame) {
        this.wallpaperFrame = frame == null ? null : frame.copy();
    }

    public void sanitize() {
        if (wallpaperFrame != null) wallpaperFrame.sanitize();
    }
}
