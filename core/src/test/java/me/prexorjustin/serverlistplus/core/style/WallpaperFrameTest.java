package me.prexorjustin.serverlistplus.core.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WallpaperFrameTest {
    @Test
    void valuesAreClamped() {
        WallpaperFrame frame = new WallpaperFrame();
        frame.setFocus(-1, 2);
        frame.setZoom(100);
        frame.setDim(-0.5);
        assertEquals(0, frame.x());
        assertEquals(1, frame.y());
        assertEquals(WallpaperFrame.MAX_ZOOM, frame.zoom());
        assertEquals(0, frame.dim());
        frame.setZoom(0.01);
        assertEquals(WallpaperFrame.MIN_ZOOM, frame.zoom());
    }

    @Test
    void defaultCoversArea() {
        assertEquals(1.0, new WallpaperFrame().zoom());
    }

    @Test
    void cutoutDefaultsToServerIconsOnly() {
        WallpaperFrame frame = new WallpaperFrame();
        assertEquals(true, frame.cutout(Wallpaper.Kind.SERVER_ICON));
        assertEquals(false, frame.cutout(Wallpaper.Kind.IMAGE));
        frame.setCutout(true);
        assertEquals(true, frame.cutout(Wallpaper.Kind.IMAGE));
        assertEquals(true, frame.copy().cutout(Wallpaper.Kind.IMAGE));
    }

    @Test
    void copyIsIndependent() {
        WallpaperFrame frame = new WallpaperFrame();
        WallpaperFrame copy = frame.copy();
        frame.setZoom(3);
        assertEquals(1.0, copy.zoom());
    }
}
