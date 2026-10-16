package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.platform.Platform;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.compat.FileManager;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Ids;
import me.prexorjustin.serverlistplus.compat.Textures;
import me.prexorjustin.serverlistplus.core.image.ImageOps;
import me.prexorjustin.serverlistplus.core.style.Palette;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.core.style.Wallpaper;
import me.prexorjustin.serverlistplus.core.style.WallpaperFrame;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Draws the background of the open tab and keeps the textures it needs.
 * Images are prepared once per content (file change or new server icon) and then reused.
 */
public final class Wallpapers {
    private static final int MAX_IMAGE_SIZE = 1280;
    private static final int ICON_SIZE = 256;
    private static final int ICON_BLUR = 10;
    private static final int GRADIENT_TOP_ALPHA = 0x55;
    private static final int GRADIENT_BOTTOM_ALPHA = 0x08;

    private record Texture(Identifier id, int width, int height) {
    }

    /** Loaded textures by content key. */
    private static final Map<String, Texture> TEXTURES = new HashMap<>();
    /** Texture drawn last per tab, used to move the picture by mouse. */
    private static final Map<String, Texture> CURRENT = new HashMap<>();
    /** Content key currently shown per tab, to release what is no longer needed. */
    private static final Map<String, String> SHOWN = new HashMap<>();
    /** Content that failed to load, so it is not retried every frame. */
    private static final Set<String> FAILED = new HashSet<>();
    private static int nextId;

    private Wallpapers() {
    }

    public static Path directory() {
        return Platform.INSTANCE.configDir().resolve(ServerListPlus.MOD_ID).resolve("wallpapers");
    }

    /** Image files in the wallpaper directory, sorted by name. */
    public static List<String> files() {
        Path directory = directory();
        if (!Files.isDirectory(directory)) return List.of();
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> {
                    String lower = name.toLowerCase(Locale.ROOT);
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        } catch (IOException e) {
            ServerListPlus.LOGGER.warn("Could not list wallpapers in {}", directory, e);
            return List.of();
        }
    }

    public static void openDirectory() {
        try {
            Files.createDirectories(directory());
            FileManager.open(directory());
        } catch (IOException e) {
            ServerListPlus.LOGGER.warn("Could not create {}", directory(), e);
        }
    }

    /** Draws the background of the current tab into the list area. */
    public static void draw(Gfx gfx, int x, int y, int width, int height, ServerList servers) {
        String tab = ServerView.tab();
        Styled style = ServerListPlus.store().style(tab);
        if (style == null) return;
        Texture texture = switch (style.wallpaper().kind()) {
            case NONE, GRADIENT -> null;
            case IMAGE -> imageTexture(tab, style.wallpaper().file(), style.wallpaperFrame().cutout(Wallpaper.Kind.IMAGE));
            case SERVER_ICON -> iconTexture(tab, servers, style.wallpaperFrame().cutout(Wallpaper.Kind.SERVER_ICON));
        };
        CURRENT.put(tab, texture);
        if (style.wallpaper().kind() == Wallpaper.Kind.NONE) return;
        if (texture == null) {
            drawGradient(gfx, x, y, width, height, style.color());
            return;
        }
        // Around a zoomed-out picture the normal list background stays visible
        WallpaperFrame frame = style.wallpaperFrame();
        ImageOps.Placement p = ImageOps.place(texture.width(), texture.height(), width, height, frame.x(), frame.y(), frame.zoom());
        if (p == null) return;
        int left = x + p.x();
        int top = y + p.y();
        // Darken the picture itself, so transparent parts of a cut-out logo stay clear
        int brightness = (int) Math.round((1 - frame.dim()) * 255);
        int tint = 0xFF000000 | brightness * 0x010101;
        gfx.image(texture.id(), left, top, p.width(), p.height(),
            p.u(), p.v(), p.partWidth(), p.partHeight(), texture.width(), texture.height(), tint);
    }

    /** Whether the tab shows a picture that can be moved and zoomed. */
    public static boolean isAdjustable(Styled style) {
        Wallpaper.Kind kind = style.wallpaper().kind();
        return kind == Wallpaper.Kind.IMAGE || kind == Wallpaper.Kind.SERVER_ICON;
    }

    /**
     * Moves the picture of a tab by a mouse movement, so it follows the pointer.
     *
     * @param width  width of the area the picture is drawn in
     * @param height height of the area the picture is drawn in
     */
    public static void pan(String tab, Styled style, int width, int height, double dx, double dy) {
        Texture texture = CURRENT.get(tab);
        if (texture == null) return;
        WallpaperFrame frame = style.wallpaperFrame();
        double[] focus = ImageOps.pan(texture.width(), texture.height(), width, height, frame.x(), frame.y(), frame.zoom(), dx, dy);
        frame.setFocus(focus[0], focus[1]);
    }

    /** Zooms the picture of a tab, never below showing it completely. */
    public static void zoom(String tab, Styled style, int width, int height, double factor) {
        WallpaperFrame frame = style.wallpaperFrame();
        Texture texture = CURRENT.get(tab);
        double min = texture == null ? 1.0 : ImageOps.minZoom(texture.width(), texture.height(), width, height);
        frame.setZoom(Math.max(min, Math.max(frame.zoom(), min) * factor));
    }

    private static void drawGradient(Gfx gfx, int x, int y, int width, int height, Palette.Color color) {
        if (color == null) return;
        gfx.gradient(x, y, x + width, y + height, color.argb(GRADIENT_TOP_ALPHA), color.argb(GRADIENT_BOTTOM_ALPHA));
    }

    private static Texture imageTexture(String tab, String file, boolean cutout) {
        Path path = directory().resolve(file).normalize();
        if (!path.startsWith(directory()) || !Files.isRegularFile(path)) return null;
        try {
            String key = "image:" + path + ":" + Files.getLastModifiedTime(path).toMillis() + ":" + cutout;
            return load(tab, key, () -> ImageOps.prepareImage(Files.readAllBytes(path), MAX_IMAGE_SIZE, cutout));
        } catch (IOException e) {
            return null;
        }
    }

    /** The first server of the tab that has an icon. */
    private static Texture iconTexture(String tab, ServerList servers, boolean cutout) {
        for (int i = 0; i < servers.size(); i++) {
            ServerData server = servers.get(i);
            byte[] icon = server.getIconBytes();
            if (icon == null || !ServerView.inTab(server, tab)) continue;
            String key = "icon:" + Arrays.hashCode(icon) + ":" + cutout;
            return load(tab, key, () -> ImageOps.prepareIcon(icon, ICON_SIZE, ICON_BLUR, cutout));
        }
        return null;
    }

    private interface Source {
        byte[] png() throws IOException;
    }

    private static Texture load(String tab, String key, Source source) {
        Texture texture = TEXTURES.get(key);
        if (texture == null && !FAILED.contains(key)) {
            try {
                byte[] png = source.png();
                BufferedImage size = ImageOps.read(png);
                Identifier id = Ids.of("wallpaper/" + nextId++);
                Textures.register(id, png);
                texture = new Texture(id, size.getWidth(), size.getHeight());
                TEXTURES.put(key, texture);
            } catch (IOException | RuntimeException e) {
                ServerListPlus.LOGGER.warn("Could not load wallpaper {}", key, e);
                FAILED.add(key);
                return null;
            }
        }
        String previous = SHOWN.put(tab, key);
        if (previous != null && !previous.equals(key) && !SHOWN.containsValue(previous)) {
            Texture old = TEXTURES.remove(previous);
            if (old != null) Textures.release(old.id());
        }
        return texture;
    }
}
