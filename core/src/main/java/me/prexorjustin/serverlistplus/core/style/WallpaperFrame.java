package me.prexorjustin.serverlistplus.core.style;

/**
 * Which part of a wallpaper picture is shown and how dark it is.
 */
public final class WallpaperFrame {
    /** Lowest stored zoom; the picture never gets smaller than fitting the area completely. */
    public static final double MIN_ZOOM = 0.05;
    public static final double MAX_ZOOM = 6.0;
    public static final double DEFAULT_DIM = 0.5;

    /** Horizontal focus, 0 = left edge of the picture, 1 = right edge. */
    private double x = 0.5;
    /** Vertical focus, 0 = top edge, 1 = bottom edge. */
    private double y = 0.5;
    /** 1 = the picture just covers the area, larger values show less of it, smaller ones all of it. */
    private double zoom = 1.0;
    /** How much the picture is darkened for readable text, 0 = not at all, 1 = black. */
    private double dim = DEFAULT_DIM;
    /** Remove the plain background of the picture; null = default for the wallpaper kind. */
    private Boolean cutout;

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setFocus(double x, double y) {
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
    }

    public double zoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = clamp(zoom, MIN_ZOOM, MAX_ZOOM);
    }

    public double dim() {
        return dim;
    }

    public void setDim(double dim) {
        this.dim = clamp(dim, 0, 1);
    }

    /** Whether the plain background of the picture is removed; by default only for server icons. */
    public boolean cutout(Wallpaper.Kind kind) {
        return cutout != null ? cutout : kind == Wallpaper.Kind.SERVER_ICON;
    }

    public void setCutout(boolean cutout) {
        this.cutout = cutout;
    }

    public WallpaperFrame copy() {
        WallpaperFrame copy = new WallpaperFrame();
        copy.x = x;
        copy.y = y;
        copy.zoom = zoom;
        copy.dim = dim;
        copy.cutout = cutout;
        return copy;
    }

    /** Fixes values from older or hand-edited files. */
    public void sanitize() {
        setFocus(Double.isFinite(x) ? x : 0.5, Double.isFinite(y) ? y : 0.5);
        setZoom(Double.isFinite(zoom) ? zoom : 1.0);
        setDim(Double.isFinite(dim) ? dim : DEFAULT_DIM);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
