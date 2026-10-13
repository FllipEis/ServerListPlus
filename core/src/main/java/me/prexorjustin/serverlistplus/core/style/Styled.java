package me.prexorjustin.serverlistplus.core.style;

/**
 * The look of a tab: folders as well as the built-in "All" tab.
 */
public interface Styled {
    /** Null if the tab has no color. */
    Palette.Color color();

    void setColor(Palette.Color color);

    /** A symbol from {@link FolderIcons}, null for none. */
    String icon();

    void setIcon(String icon);

    Wallpaper wallpaper();

    void setWallpaper(Wallpaper wallpaper);

    WallpaperFrame wallpaperFrame();

    void setWallpaperFrame(WallpaperFrame frame);
}
