package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerSelectionListHook;
import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.ui.serverlist.Wallpapers;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The background drawing lives in the shared list class; only the server list gets a wallpaper,
 * and it makes sure the rows are where the list is before they are drawn.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin {
    //? if >=26.1 {
    @Inject(method = "extractListBackground", at = @At("TAIL"))
    //?} else
    /*@Inject(method = "renderListBackground", at = @At("TAIL"))*/
    private void serverlistplus$beforeRows(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (!((Object) this instanceof ServerSelectionList list)) return;
        serverlistplus$keepRowsInPlace(list);
        MultiplayerScreenHook screen = (MultiplayerScreenHook) ((ServerSelectionListHook) list).serverlistplus$screen();
        Wallpapers.draw(new Gfx(graphics), list.getX(), list.getY(), list.getWidth(), list.getHeight(), screen.serverlistplus$servers());
    }

    /**
     * Since 1.21.9 rows store their position and only take a new one when the list is resized or
     * scrolled. Coming back from a server, the server list (shifted by the tab sidebar) was seen
     * with its rows still where the list used to be, until the next scroll. Doing what scrolling
     * does whenever they are out of place keeps them under the list, whatever moved it.
     */
    @Unique
    private static void serverlistplus$keepRowsInPlace(ServerSelectionList list) {
        //? if >=1.21.9 {
        if (!list.children().isEmpty() && list.children().getFirst().getX() != list.getRowLeft()) {
            list.setScrollAmount(list.scrollAmount());
        }
        //?}
    }
}
