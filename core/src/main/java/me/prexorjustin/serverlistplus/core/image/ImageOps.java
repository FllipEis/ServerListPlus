package me.prexorjustin.serverlistplus.core.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Image preparation for wallpapers, done with plain Java so it works the same on every Minecraft
 * version and reads JPEG as well as PNG. The result is handed to Minecraft as PNG.
 */
public final class ImageOps {
    private ImageOps() {
    }

    public static BufferedImage read(byte[] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) throw new IOException("Unsupported image format");
        return toArgb(image);
    }

    private static BufferedImage toArgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) return image;
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return copy;
    }

    /** Scales down (never up) so neither side exceeds {@code maxSize}, keeping the aspect ratio. */
    public static BufferedImage fit(BufferedImage image, int maxSize) {
        double scale = Math.min(1.0, (double) maxSize / Math.max(image.getWidth(), image.getHeight()));
        if (scale >= 1.0) return image;
        return resize(image, Math.max(1, (int) Math.round(image.getWidth() * scale)), Math.max(1, (int) Math.round(image.getHeight() * scale)));
    }

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    /**
     * Box blur, applied three times which looks close to a Gaussian blur.
     * Edges are clamped, so the borders don't darken.
     */
    public static BufferedImage blur(BufferedImage image, int radius) {
        if (radius <= 0) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int[] buffer = new int[pixels.length];
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(pixels, buffer, width, height, radius, true);
            boxBlur(buffer, pixels, width, height, radius, false);
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, pixels, 0, width);
        return result;
    }

    private static void boxBlur(int[] in, int[] out, int width, int height, int radius, boolean horizontal) {
        int lines = horizontal ? height : width;
        int length = horizontal ? width : height;
        int span = 2 * radius + 1;
        for (int line = 0; line < lines; line++) {
            long a = 0, r = 0, g = 0, b = 0;
            for (int i = -radius; i <= radius; i++) {
                int p = in[index(line, clamp(i, length), width, horizontal)];
                a += p >>> 24;
                r += p >> 16 & 0xFF;
                g += p >> 8 & 0xFF;
                b += p & 0xFF;
            }
            for (int i = 0; i < length; i++) {
                out[index(line, i, width, horizontal)] = (int) (a / span) << 24 | (int) (r / span) << 16 | (int) (g / span) << 8 | (int) (b / span);
                int leaving = in[index(line, clamp(i - radius, length), width, horizontal)];
                int entering = in[index(line, clamp(i + radius + 1, length), width, horizontal)];
                a += (entering >>> 24) - (leaving >>> 24);
                r += (entering >> 16 & 0xFF) - (leaving >> 16 & 0xFF);
                g += (entering >> 8 & 0xFF) - (leaving >> 8 & 0xFF);
                b += (entering & 0xFF) - (leaving & 0xFF);
            }
        }
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(length - 1, value));
    }

    private static int index(int line, int position, int width, boolean horizontal) {
        return horizontal ? line * width + position : position * width + line;
    }

    /**
     * Makes the plain background of a picture transparent, e.g. the square behind a server logo.
     * The background color is the most common color along the border; everything connected to the
     * border that is close to it is removed, with a soft edge. Pictures without a clear background
     * (more than {@code maxRemoved} of the pixels would vanish) are returned unchanged.
     *
     * @param tolerance how far (RGB distance, 0-441) a color may be from the background to count as background
     */
    public static BufferedImage removeBackground(BufferedImage image, int tolerance, double maxRemoved) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int background = dominantBorderColor(pixels, width, height);

        boolean[] removed = new boolean[pixels.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            seed(queue, removed, pixels, x, background, tolerance);
            seed(queue, removed, pixels, (height - 1) * width + x, background, tolerance);
        }
        for (int y = 0; y < height; y++) {
            seed(queue, removed, pixels, y * width, background, tolerance);
            seed(queue, removed, pixels, y * width + width - 1, background, tolerance);
        }
        int count = 0;
        while (!queue.isEmpty()) {
            int index = queue.poll();
            count++;
            int x = index % width;
            int y = index / width;
            if (x > 0) seed(queue, removed, pixels, index - 1, background, tolerance);
            if (x < width - 1) seed(queue, removed, pixels, index + 1, background, tolerance);
            if (y > 0) seed(queue, removed, pixels, index - width, background, tolerance);
            if (y < height - 1) seed(queue, removed, pixels, index + width, background, tolerance);
        }
        if (count == 0 || count > maxRemoved * pixels.length) return image;

        int[] result = pixels.clone();
        for (int i = 0; i < pixels.length; i++) {
            if (removed[i]) {
                result[i] = 0;
            } else if (touches(removed, i, width, height)) {
                // Soften the edge: colors still close to the background become partly transparent
                double distance = distance(pixels[i], background);
                double keep = Math.min(1.0, Math.max(0.0, (distance - tolerance) / tolerance));
                int alpha = (int) Math.round((pixels[i] >>> 24) * keep);
                result[i] = alpha << 24 | pixels[i] & 0xFFFFFF;
            }
        }
        BufferedImage cut = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        cut.setRGB(0, 0, width, height, result, 0, width);
        return cut;
    }

    private static void seed(ArrayDeque<Integer> queue, boolean[] removed, int[] pixels, int index, int background, int tolerance) {
        if (removed[index]) return;
        int pixel = pixels[index];
        if ((pixel >>> 24) < 16 || distance(pixel, background) <= tolerance) {
            removed[index] = true;
            queue.add(index);
        }
    }

    private static boolean touches(boolean[] removed, int index, int width, int height) {
        int x = index % width;
        int y = index / width;
        return x > 0 && removed[index - 1] || x < width - 1 && removed[index + 1]
            || y > 0 && removed[index - width] || y < height - 1 && removed[index + width];
    }

    private static double distance(int a, int b) {
        int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
        int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /** Most frequent border color, with colors grouped in steps of 16 per channel. */
    private static int dominantBorderColor(int[] pixels, int width, int height) {
        Map<Integer, int[]> buckets = new HashMap<>();
        java.util.function.IntConsumer count = index -> {
            int pixel = pixels[index];
            int key = (pixel >> 20 & 0xF) << 8 | (pixel >> 12 & 0xF) << 4 | (pixel >> 4 & 0xF);
            int[] bucket = buckets.computeIfAbsent(key, k -> new int[]{0, pixel});
            bucket[0]++;
        };
        for (int x = 0; x < width; x++) {
            count.accept(x);
            count.accept((height - 1) * width + x);
        }
        for (int y = 1; y < height - 1; y++) {
            count.accept(y * width);
            count.accept(y * width + width - 1);
        }
        return buckets.values().stream().max(Comparator.comparingInt(bucket -> bucket[0])).orElseThrow()[1];
    }

    public static byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final int CUTOUT_TOLERANCE = 48;
    private static final double CUTOUT_MAX_REMOVED = 0.9;

    /**
     * A wallpaper from a user image: limited to {@code maxSize} pixels per side.
     *
     * @param cutout remove a plain background, see {@link #removeBackground}
     */
    public static byte[] prepareImage(byte[] data, int maxSize, boolean cutout) throws IOException {
        BufferedImage image = fit(read(data), maxSize);
        if (cutout) image = removeBackground(image, CUTOUT_TOLERANCE, CUTOUT_MAX_REMOVED);
        return toPng(image);
    }

    /**
     * A wallpaper from a small server icon, scaled up smoothly. Without cutout it is heavily
     * blurred into a soft backdrop; with cutout the logo stays sharp on a transparent background.
     */
    public static byte[] prepareIcon(byte[] iconPng, int size, int blurRadius, boolean cutout) throws IOException {
        BufferedImage icon = read(iconPng);
        if (cutout) return toPng(resize(removeBackground(icon, CUTOUT_TOLERANCE, CUTOUT_MAX_REMOVED), size, size));
        return toPng(blur(resize(icon, size, size), blurRadius));
    }

    /**
     * The part of an image to show so it covers a target area without distortion (like CSS
     * {@code background-size: cover}), centered.
     *
     * @return {@code {u, v, width, height}} in image pixels
     */
    public static int[] coverCrop(int imageWidth, int imageHeight, int targetWidth, int targetHeight) {
        return coverCrop(imageWidth, imageHeight, targetWidth, targetHeight, 0.5, 0.5, 1.0);
    }

    /**
     * Like {@link #coverCrop(int, int, int, int)}, but zoomed in and moved.
     *
     * @param focusX 0 shows the left edge of the image, 1 the right edge
     * @param focusY 0 shows the top edge, 1 the bottom edge
     * @param zoom   1 just covers the area, 2 shows half as much in each direction
     * @return {@code {u, v, width, height}} in image pixels
     */
    public static int[] coverCrop(int imageWidth, int imageHeight, int targetWidth, int targetHeight,
                                  double focusX, double focusY, double zoom) {
        double targetRatio = (double) targetWidth / Math.max(1, targetHeight);
        double width = imageWidth;
        double height = imageWidth / targetRatio;
        if (height > imageHeight) {
            height = imageHeight;
            width = imageHeight * targetRatio;
        }
        zoom = Math.max(1.0, zoom);
        int cropWidth = Math.max(1, (int) Math.round(width / zoom));
        int cropHeight = Math.max(1, (int) Math.round(height / zoom));
        int u = (int) Math.round((imageWidth - cropWidth) * clamp01(focusX));
        int v = (int) Math.round((imageHeight - cropHeight) * clamp01(focusY));
        return new int[]{u, v, cropWidth, cropHeight};
    }

    /**
     * Where an image is drawn inside an area: the visible part of the image and the rectangle it covers.
     * All values are relative to the area's top left corner (screen) or the image's (u, v).
     */
    public record Placement(int x, int y, int width, int height, int u, int v, int partWidth, int partHeight) {
    }

    /** Zoom (relative to covering the area) at which the whole image just fits inside it. */
    public static double minZoom(int imageWidth, int imageHeight, int areaWidth, int areaHeight) {
        double fit = Math.min((double) areaWidth / imageWidth, (double) areaHeight / imageHeight);
        double cover = Math.max((double) areaWidth / imageWidth, (double) areaHeight / imageHeight);
        return fit / cover;
    }

    /**
     * Places an image in an area without distortion.
     *
     * @param zoom   1 just covers the area; smaller shows the whole image with free space around it
     *               (down to {@link #minZoom}), larger zooms in
     * @param focusX 0 aligns the image's left edge with the area's left edge, 1 the right edges;
     *               works the same whether the image is wider or narrower than the area
     * @param focusY like {@code focusX}, vertically
     * @return null if nothing of the image is visible
     */
    public static Placement place(int imageWidth, int imageHeight, int areaWidth, int areaHeight,
                                  double focusX, double focusY, double zoom) {
        double cover = Math.max((double) areaWidth / imageWidth, (double) areaHeight / imageHeight);
        double scale = cover * Math.max(zoom, minZoom(imageWidth, imageHeight, areaWidth, areaHeight));
        double drawnWidth = imageWidth * scale;
        double drawnHeight = imageHeight * scale;
        double left = (areaWidth - drawnWidth) * clamp01(focusX);
        double top = (areaHeight - drawnHeight) * clamp01(focusY);

        // Clip the drawn image to the area and map the visible rectangle back to image pixels
        double x1 = Math.max(0, left);
        double y1 = Math.max(0, top);
        double x2 = Math.min(areaWidth, left + drawnWidth);
        double y2 = Math.min(areaHeight, top + drawnHeight);
        if (x2 - x1 < 1 || y2 - y1 < 1) return null;
        int u = (int) Math.round((x1 - left) / scale);
        int v = (int) Math.round((y1 - top) / scale);
        int partWidth = Math.max(1, Math.min(imageWidth - u, (int) Math.round((x2 - x1) / scale)));
        int partHeight = Math.max(1, Math.min(imageHeight - v, (int) Math.round((y2 - y1) / scale)));
        return new Placement((int) Math.round(x1), (int) Math.round(y1),
            (int) Math.round(x2 - x1), (int) Math.round(y2 - y1), u, v, partWidth, partHeight);
    }

    /**
     * Focus change that moves the image by a mouse movement, so it follows the pointer.
     *
     * @return {@code {newFocusX, newFocusY}}
     */
    public static double[] pan(int imageWidth, int imageHeight, int areaWidth, int areaHeight,
                               double focusX, double focusY, double zoom, double dx, double dy) {
        double cover = Math.max((double) areaWidth / imageWidth, (double) areaHeight / imageHeight);
        double scale = cover * Math.max(zoom, minZoom(imageWidth, imageHeight, areaWidth, areaHeight));
        double freeX = areaWidth - imageWidth * scale;
        double freeY = areaHeight - imageHeight * scale;
        // The image's left edge is at freeX * focus, so moving it by dx changes focus by dx / freeX
        double x = Math.abs(freeX) >= 1 ? focusX + dx / freeX : focusX;
        double y = Math.abs(freeY) >= 1 ? focusY + dy / freeY : focusY;
        return new double[]{clamp01(x), clamp01(y)};
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
