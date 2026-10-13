package me.prexorjustin.serverlistplus.core.style;

import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.Store;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StyleTest {
    @Test
    void wallpaperRoundTrip() {
        for (Wallpaper wallpaper : new Wallpaper[]{Wallpaper.NONE, Wallpaper.GRADIENT, Wallpaper.SERVER_ICON, Wallpaper.image("my server.png")}) {
            assertEquals(wallpaper, Wallpaper.parse(wallpaper.serialize()));
        }
    }

    @Test
    void unknownWallpaperIsGradient() {
        assertEquals(Wallpaper.GRADIENT, Wallpaper.parse(null));
        assertEquals(Wallpaper.GRADIENT, Wallpaper.parse("video:foo"));
        assertEquals(Wallpaper.GRADIENT, Wallpaper.parse("image:"));
    }

    @Test
    void newFoldersGetDifferentColors() {
        Store store = new Store();
        Folder a = store.addFolder(new Folder("A"));
        Folder b = store.addFolder(new Folder("B"));
        assertNotEquals(a.color(), b.color());
    }

    @Test
    void sanitizeColorsOldFolders() {
        Store store = new Store();
        store.folders().add(new Folder("Old"));
        assertNull(store.folders().getFirst().color());
        store.sanitize();
        assertEquals(Palette.forIndex(0), store.folders().getFirst().color());
    }

    @Test
    void unknownColorKeyMeansNoColor() {
        assertEquals(java.util.Optional.empty(), Palette.byKey("ultraviolet"));
    }

    @Test
    void builtInTabsHaveStyles() {
        Store store = new Store();
        assertEquals(Wallpaper.NONE, store.style(me.prexorjustin.serverlistplus.core.model.ViewSettings.TAB_ALL).wallpaper());
        Folder folder = store.addFolder(new Folder("A"));
        assertEquals(folder, store.style(folder.id()));
        assertNull(store.style("unknown"));
    }

    @Test
    void missingBuiltInStylesAreRestored() throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("slp", ".json");
        java.nio.file.Files.writeString(file, "{\"version\": 1, \"view\": {\"sort\": \"NAME\"}}");
        Store store = new me.prexorjustin.serverlistplus.core.storage.JsonStore(file).load().store();
        assertEquals(Wallpaper.NONE, store.view().allStyle().wallpaper());
        java.nio.file.Files.delete(file);
    }

    @Test
    void argb() {
        assertEquals(0x80E5484D, Palette.byKey("red").orElseThrow().argb(0x80));
    }
}
