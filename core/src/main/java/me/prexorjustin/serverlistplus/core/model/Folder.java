package me.prexorjustin.serverlistplus.core.model;

import me.prexorjustin.serverlistplus.core.style.Palette;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.core.style.Wallpaper;
import me.prexorjustin.serverlistplus.core.style.WallpaperFrame;

import java.util.UUID;

/**
 * A user-defined group of servers, shown as a tab. Display order is the order in {@link Store#folders()}.
 * <p>
 * A folder with a {@link #query()} is a smart folder: it contains every server matching the
 * saved search instead of servers that were put into it.
 */
public final class Folder implements Styled {
    private String id;
    private String name;
    private String query;
    /** Key of a {@link Palette} color. */
    private String color;
    /** A symbol from {@link me.prexorjustin.serverlistplus.core.style.FolderIcons}, null for none. */
    private String icon;
    /** See {@link Wallpaper}; null means the default. */
    private String wallpaper;
    private WallpaperFrame wallpaperFrame;
    /** Unused since folders became tabs; kept so older files load unchanged. */
    private boolean collapsed;

    public Folder(String name) {
        this(name, null);
    }

    public Folder(String name, String query) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        setQuery(query);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The saved search of a smart folder, null for a normal folder. */
    public String query() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null || query.isBlank() ? null : query.trim();
    }

    public boolean isSmart() {
        return query != null;
    }

    /** The folder color, or null if it has none (e.g. older files before a color was assigned). */
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

    /** Position, zoom and darkness of the wallpaper picture. */
    @Override
    public WallpaperFrame wallpaperFrame() {
        if (wallpaperFrame == null) wallpaperFrame = new WallpaperFrame();
        return wallpaperFrame;
    }

    @Override
    public void setWallpaperFrame(WallpaperFrame frame) {
        this.wallpaperFrame = frame == null ? null : frame.copy();
    }

    boolean isValid() {
        return id != null && !id.isBlank();
    }

    void sanitize() {
        if (name == null) name = "";
        setQuery(query);
        if (wallpaperFrame != null) wallpaperFrame.sanitize();
    }
}
