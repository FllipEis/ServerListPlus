package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.core.style.FolderIcons;
import me.prexorjustin.serverlistplus.core.style.Palette;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.core.style.Wallpaper;
import me.prexorjustin.serverlistplus.ui.widget.ContextMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Submenus for the look of tabs and dividers: color, symbol and background.
 */
final class StyleMenus {
    private final UiContext ui;
    /** Starts adjusting the wallpaper picture of a tab. */
    private final BiConsumer<String, Styled> adjustWallpaper;

    StyleMenus(UiContext ui, BiConsumer<String, Styled> adjustWallpaper) {
        this.ui = ui;
        this.adjustWallpaper = adjustWallpaper;
    }

    /**
     * "None" plus every palette color, each previewed with a dot.
     *
     * @param noneKey translation key of the entry without a color
     */
    List<ContextMenu.Item> colors(Palette.Color current, Consumer<Palette.Color> set, String noneKey) {
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.action(Component.translatable(noneKey), () -> {
            set.accept(null);
            ui.changed();
        }).checked(current == null));
        for (Palette.Color color : Palette.COLORS) {
            Component label = Component.literal("● ").withStyle(s -> s.withColor(TextColor.fromRgb(color.rgb())))
                .append(Component.translatable("serverlistplus.color." + color.key()));
            items.add(ContextMenu.Item.action(label, () -> {
                set.accept(color);
                ui.changed();
            }).checked(color.equals(current)));
        }
        return items;
    }

    List<ContextMenu.Item> icons(Styled style) {
        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(ContextMenu.Item.action(Component.translatable("serverlistplus.icon.none"), () -> {
            style.setIcon(null);
            ui.changed();
        }).checked(style.icon() == null));
        for (String icon : FolderIcons.ICONS) {
            items.add(ContextMenu.Item.action(Component.literal(icon), () -> {
                style.setIcon(icon);
                ui.changed();
            }).checked(icon.equals(style.icon())));
        }
        return items;
    }

    /** Off / gradient / server icon / Picture ▸ files, plus adjusting the picture. */
    List<ContextMenu.Item> backgrounds(String tab, Styled style) {
        Wallpaper current = style.wallpaper();
        List<ContextMenu.Item> pictures = new ArrayList<>();
        for (String file : Wallpapers.files()) {
            pictures.add(wallpaper(style, Wallpaper.image(file), Component.literal(file), current));
        }
        if (!pictures.isEmpty()) pictures.add(ContextMenu.Item.separator());
        pictures.add(ContextMenu.Item.action(Component.translatable("serverlistplus.wallpaper.open"), Wallpapers::openDirectory));

        List<ContextMenu.Item> items = new ArrayList<>();
        items.add(wallpaper(style, Wallpaper.NONE, Component.translatable("serverlistplus.wallpaper.none"), current));
        items.add(wallpaper(style, Wallpaper.GRADIENT, Component.translatable("serverlistplus.wallpaper.gradient"), current));
        items.add(wallpaper(style, Wallpaper.SERVER_ICON, Component.translatable("serverlistplus.wallpaper.icon"), current));
        items.add(ContextMenu.Item.submenu(Component.translatable("serverlistplus.wallpaper.picture"), pictures)
            .checked(current.kind() == Wallpaper.Kind.IMAGE));
        items.add(ContextMenu.Item.separator());
        boolean picture = Wallpapers.isAdjustable(style);
        boolean cutout = picture && style.wallpaperFrame().cutout(current.kind());
        items.add(new ContextMenu.Item(Component.translatable("serverlistplus.wallpaper.cutout"), () -> {
            style.wallpaperFrame().setCutout(!cutout);
            ui.changed();
        }, List.of(), picture).checked(cutout));
        items.add(new ContextMenu.Item(Component.translatable("serverlistplus.wallpaper.adjust"),
            () -> adjustWallpaper.accept(tab, style), List.of(), picture));
        return items;
    }

    private ContextMenu.Item wallpaper(Styled style, Wallpaper wallpaper, Component label, Wallpaper current) {
        return ContextMenu.Item.action(label, () -> {
            style.setWallpaper(wallpaper);
            ui.changed();
        }).checked(wallpaper.equals(current));
    }
}
