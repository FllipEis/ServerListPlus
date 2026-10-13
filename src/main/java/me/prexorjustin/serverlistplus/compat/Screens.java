package me.prexorjustin.serverlistplus.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Screens {
    private Screens() {
    }

    public static void open(Screen screen) {
        //? if >=26.2 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else
        /*Minecraft.getInstance().setScreen(screen);*/
    }
}
