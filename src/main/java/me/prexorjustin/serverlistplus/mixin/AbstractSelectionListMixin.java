package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerSelectionListHook;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.ui.Wallpapers;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The background drawing lives in the shared list class; only the server list gets a wallpaper.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
    //? if >=26.1 {
    @Inject(method = "extractListBackground", at = @At("TAIL"))
    //?} else
    /*@Inject(method = "renderListBackground", at = @At("TAIL"))*/
    private void serverlistplus$wallpaper(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        serverlistplus$drawWallpaper(new Gfx(graphics));
    }

    @Unique
    private void serverlistplus$drawWallpaper(Gfx gfx) {
        if (!((Object) this instanceof ServerSelectionList list)) return;
        MultiplayerScreenHook screen = (MultiplayerScreenHook) ((ServerSelectionListHook) list).serverlistplus$screen();
        Wallpapers.draw(gfx, list.getX(), list.getY(), list.getWidth(), list.getHeight(), screen.serverlistplus$servers());
    }
}
