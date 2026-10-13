package me.prexorjustin.serverlistplus.compat;

import java.nio.file.Path;
//? if >=26.3 {
import com.mojang.blaze3d.Blaze3D;
//?} elif >=1.21.11 {
/*import net.minecraft.util.Util;
*///?} else
/*import net.minecraft.Util;*/

/**
 * The operating system's file manager.
 */
public final class FileManager {
    private FileManager() {
    }

    /** Opens a directory in the system file manager. */
    public static void open(Path directory) {
        //? if >=26.3 {
        Blaze3D.openPath(directory);
        //?} elif >=1.21 {
        /*Util.getPlatform().openPath(directory);
        *///?} else
        /*Util.getPlatform().openFile(directory.toFile());*/
    }
}
