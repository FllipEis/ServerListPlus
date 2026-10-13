package me.prexorjustin.serverlistplus.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;

/**
 * Turns PNG data into textures that can be drawn with {@link Gfx#image}.
 */
public final class Textures {
    private Textures() {
    }

    public static void register(Identifier id, byte[] png) throws IOException {
        NativeImage image = NativeImage.read(png);
        //? if >=1.21.5 {
        DynamicTexture texture = new DynamicTexture(id::toString, image);
        //?} else
        /*DynamicTexture texture = new DynamicTexture(image);*/
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    public static void release(Identifier id) {
        Minecraft.getInstance().getTextureManager().release(id);
    }
}
