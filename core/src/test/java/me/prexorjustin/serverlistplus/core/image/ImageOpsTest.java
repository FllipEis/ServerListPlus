package me.prexorjustin.serverlistplus.core.image;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageOpsTest {
    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage halves(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) image.setRGB(x, y, x < width / 2 ? 0x000000 : 0xFFFFFF);
        }
        return image;
    }

    @Test
    void readsJpegAndPng() throws IOException {
        assertEquals(40, ImageOps.read(encode(halves(40, 20), "jpg")).getWidth());
        assertEquals(40, ImageOps.read(encode(halves(40, 20), "png")).getWidth());
    }

    @Test
    void rejectsNonImages() {
        assertThrows(IOException.class, () -> ImageOps.read(new byte[]{1, 2, 3}));
    }

    @Test
    void fitOnlyShrinks() {
        BufferedImage large = ImageOps.fit(halves(4000, 2000), 1000);
        assertEquals(1000, large.getWidth());
        assertEquals(500, large.getHeight());
        assertEquals(40, ImageOps.fit(halves(40, 20), 1000).getWidth());
    }

    @Test
    void blurSoftensEdgeButKeepsFarPixels() {
        BufferedImage blurred = ImageOps.blur(halves(64, 8), 3);
        int atEdge = blurred.getRGB(32, 4) & 0xFF;
        assertTrue(atEdge > 30 && atEdge < 225, "edge pixel is mixed, was " + atEdge);
        assertEquals(0x00, blurred.getRGB(0, 4) & 0xFF);
        assertEquals(0xFF, blurred.getRGB(63, 4) & 0xFF);
    }

    @Test
    void preparedIconIsPngOfRequestedSize() throws IOException {
        byte[] png = ImageOps.prepareIcon(encode(halves(64, 64), "png"), 128, 8, false);
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());
    }

    @Test
    void coverCropWideImage() {
        // 200x100 image into a square: take the centered 100x100
        assertArrayEquals(new int[]{50, 0, 100, 100}, ImageOps.coverCrop(200, 100, 50, 50));
    }

    @Test
    void zoomShowsLessAroundFocus() {
        // 200x100 image into a square at 2x zoom: 50x50, centered
        assertArrayEquals(new int[]{75, 25, 50, 50}, ImageOps.coverCrop(200, 100, 50, 50, 0.5, 0.5, 2.0));
    }

    @Test
    void focusMovesToEdges() {
        assertArrayEquals(new int[]{0, 0, 100, 100}, ImageOps.coverCrop(200, 100, 50, 50, 0.0, 0.0, 1.0));
        assertArrayEquals(new int[]{100, 0, 100, 100}, ImageOps.coverCrop(200, 100, 50, 50, 1.0, 1.0, 1.0));
        assertArrayEquals(new int[]{150, 50, 50, 50}, ImageOps.coverCrop(200, 100, 50, 50, 1.0, 1.0, 2.0));
    }

    @Test
    void zoomBelowOneIsCover() {
        assertArrayEquals(ImageOps.coverCrop(200, 100, 50, 50), ImageOps.coverCrop(200, 100, 50, 50, 0.5, 0.5, 0.2));
    }

    @Test
    void placeCoversAtZoomOne() {
        // Portrait 100x200 image in a 400x100 area: scaled 4x to 400x800, middle strip shown
        ImageOps.Placement p = ImageOps.place(100, 200, 400, 100, 0.5, 0.5, 1.0);
        assertEquals(new ImageOps.Placement(0, 0, 400, 100, 0, 88, 100, 25), p);
    }

    @Test
    void placeKeepsAspectRatio() {
        ImageOps.Placement p = ImageOps.place(392, 700, 1000, 310, 0.5, 0.34, 1.0);
        double screenRatio = (double) p.width() / p.height();
        double imageRatio = (double) p.partWidth() / p.partHeight();
        assertEquals(screenRatio, imageRatio, 0.05);
    }

    @Test
    void zoomingOutShowsWholeImageCentered() {
        // Fits at scale 0.5: 50x100, centered in the 400x100 area
        ImageOps.Placement p = ImageOps.place(100, 200, 400, 100, 0.5, 0.5, 0.01);
        assertEquals(new ImageOps.Placement(175, 0, 50, 100, 0, 0, 100, 200), p);
    }

    @Test
    void focusPlacesSmallImageInFreeSpace() {
        ImageOps.Placement left = ImageOps.place(100, 200, 400, 100, 0.0, 0.5, 0.01);
        ImageOps.Placement right = ImageOps.place(100, 200, 400, 100, 1.0, 0.5, 0.01);
        assertEquals(0, left.x());
        assertEquals(350, right.x());
    }

    @Test
    void minZoom() {
        assertEquals(0.125, ImageOps.minZoom(100, 200, 400, 100), 1e-9);
        assertEquals(1.0, ImageOps.minZoom(400, 100, 400, 100), 1e-9);
    }

    @Test
    void panFollowsPointer() {
        // Covering: image is 400x800 in a 400x100 area, free space -700 vertically
        double[] focus = ImageOps.pan(100, 200, 400, 100, 0.5, 0.5, 1.0, 0, 70);
        assertEquals(0.4, focus[1], 1e-9);
        assertEquals(0.5, focus[0], 1e-9);
    }

    /** A red square on a dark gray background with slight noise, like a typical server logo. */
    private static BufferedImage logo() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.util.Random random = new java.util.Random(1);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                boolean logo = x >= 20 && x < 44 && y >= 20 && y < 44;
                int noise = random.nextInt(8);
                image.setRGB(x, y, logo ? 0xFFE03030 : 0xFF000000 | (0x30 + noise) * 0x010101);
            }
        }
        return image;
    }

    @Test
    void removesPlainBackgroundKeepsLogo() {
        BufferedImage cut = ImageOps.removeBackground(logo(), 48, 0.9);
        assertEquals(0, cut.getRGB(0, 0) >>> 24, "corner is transparent");
        assertEquals(0, cut.getRGB(10, 50) >>> 24, "background is transparent");
        assertEquals(0xFFE03030, cut.getRGB(32, 32), "logo is untouched");
    }

    @Test
    void keepsPicturesWithoutClearBackground() {
        BufferedImage plain = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) plain.setRGB(x, y, 0xFF336699);
        assertEquals(plain, ImageOps.removeBackground(plain, 48, 0.9));
    }

    @Test
    void enclosedBackgroundColorStays() {
        // A dark hole inside the logo is not connected to the border and must stay
        BufferedImage image = logo();
        for (int x = 30; x < 34; x++) for (int y = 30; y < 34; y++) image.setRGB(x, y, 0xFF303030);
        BufferedImage cut = ImageOps.removeBackground(image, 48, 0.9);
        assertEquals(0xFF, cut.getRGB(31, 31) >>> 24);
    }

    @Test
    void coverCropTallImage() {
        // 100x200 image into a 2:1 area: take a centered 100x50 strip
        assertArrayEquals(new int[]{0, 75, 100, 50}, ImageOps.coverCrop(100, 200, 200, 100));
    }
}
