package me.prexorjustin.serverlistplus.compat;

import me.prexorjustin.serverlistplus.ServerListPlus;
import net.minecraft.resources.Identifier;

public final class Ids {
    private Ids() {
    }

    public static Identifier of(String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(ServerListPlus.MOD_ID, path);
        //?} else
        /*return new Identifier(ServerListPlus.MOD_ID, path);*/
    }
}
