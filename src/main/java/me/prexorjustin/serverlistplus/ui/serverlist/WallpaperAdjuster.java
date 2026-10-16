package me.prexorjustin.serverlistplus.ui.serverlist;

import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.compat.Key;
import me.prexorjustin.serverlistplus.compat.Pointer;
import me.prexorjustin.serverlistplus.core.style.Styled;
import me.prexorjustin.serverlistplus.core.style.WallpaperFrame;
import me.prexorjustin.serverlistplus.ui.widget.DimSlider;
import me.prexorjustin.serverlistplus.ui.widget.HintLabel;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user place a tab's wallpaper picture: drag to move, wheel to zoom, a slider to darken it.
 * While active, a toolbar replaces the header row.
 */
final class WallpaperAdjuster {
    private static final double ZOOM_STEP = 1.1;

    private final UiContext ui;
    private final List<AbstractWidget> toolbar = new ArrayList<>();
    private Styled style;
    private String tab;
    private WallpaperFrame original;
    private boolean panning;
    private double panX;
    private double panY;

    WallpaperAdjuster(UiContext ui) {
        this.ui = ui;
    }

    boolean isActive() {
        return style != null;
    }

    /**
     * @param margin space kept free at both ends of the toolbar
     * @param y      top of the toolbar
     */
    void start(String tab, Styled style, int margin, int y) {
        this.style = style;
        this.tab = tab;
        this.original = style.wallpaperFrame().copy();
        showToolbar(margin, y);
    }

    /** Placed from the current screen width, so a rebuild after resizing fits again. */
    private void showToolbar(int left, int y) {
        int right = ui.screen().width - left;
        Button done = Button.builder(CommonComponents.GUI_DONE, button -> finish(true)).bounds(right - 60, y, 60, 20).build();
        Button reset = Button.builder(Component.translatable("serverlistplus.adjust.reset"), button -> reset(left, y))
            .bounds(right - 60 - 4 - 80, y, 80, 20).build();
        DimSlider dim = new DimSlider(reset.getX() - 4 - 110, y, 110, 20, style.wallpaperFrame());
        HintLabel hint = new HintLabel(ui.font(), Component.translatable("serverlistplus.adjust.hint"));
        hint.setRectangle(Math.max(0, dim.getX() - 6 - left), 20, left, y);
        for (AbstractWidget widget : List.of(dim, reset, done)) {
            ui.hook().serverlistplus$add(widget);
            toolbar.add(widget);
        }
        ui.hook().serverlistplus$addOverlay(hint);
        toolbar.add(hint);
    }

    private void hideToolbar() {
        toolbar.forEach(ui.hook()::serverlistplus$remove);
        toolbar.clear();
    }

    /** The slider edits the frame it was created with, so the toolbar is rebuilt for the new one. */
    private void reset(int left, int y) {
        style.setWallpaperFrame(new WallpaperFrame());
        hideToolbar();
        showToolbar(left, y);
    }

    private void finish(boolean keep) {
        if (!isActive()) return;
        if (!keep) style.setWallpaperFrame(original);
        hideToolbar();
        style = null;
        tab = null;
        panning = false;
        ServerListPlus.save();
        ui.relayout();
    }

    boolean press(Pointer pointer) {
        if (!pointer.isLeft() || !ui.list().isMouseOver(pointer.x(), pointer.y())) return false;
        panning = true;
        panX = pointer.x();
        panY = pointer.y();
        return true;
    }

    boolean drag(Pointer pointer) {
        if (!panning) return false;
        ServerSelectionList list = ui.list();
        Wallpapers.pan(tab, style, list.getWidth(), list.getHeight(), pointer.x() - panX, pointer.y() - panY);
        panX = pointer.x();
        panY = pointer.y();
        return true;
    }

    boolean release() {
        if (!panning) return false;
        panning = false;
        return true;
    }

    boolean scroll(double x, double y, double amount) {
        if (!isActive() || amount == 0 || !ui.list().isMouseOver(x, y)) return false;
        ServerSelectionList list = ui.list();
        Wallpapers.zoom(tab, style, list.getWidth(), list.getHeight(), Math.pow(ZOOM_STEP, Math.signum(amount)));
        return true;
    }

    /** Enter keeps the new placement, Escape restores the old one. */
    boolean key(Key key) {
        if (!isActive() || key != Key.CONFIRM && key != Key.ESCAPE) return false;
        finish(key == Key.CONFIRM);
        return true;
    }
}
