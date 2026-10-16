package me.prexorjustin.serverlistplus.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;

/**
 * Shows toasts in the corner of the screen.
 */
public final class Toasts {
    private Toasts() {
    }

    public static void show(Toast toast) {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >=26.2 {
        // The toasts moved to the HUD in 26.2
        minecraft.gui.toastManager().addToast(toast);
        //?} elif >=1.21.2 {
        /*minecraft.getToastManager().addToast(toast);
        *///?} else
        /*minecraft.getToasts().addToast(toast);*/
    }
}
