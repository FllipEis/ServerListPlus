package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.core.style.WallpaperFrame;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Sets how much a wallpaper picture is darkened.
 */
public final class DimSlider extends AbstractSliderButton {
    private final WallpaperFrame frame;

    public DimSlider(int x, int y, int width, int height, WallpaperFrame frame) {
        super(x, y, width, height, Component.empty(), frame.dim());
        this.frame = frame;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.translatable("serverlistplus.adjust.dim", (int) Math.round(this.value * 100)));
    }

    @Override
    protected void applyValue() {
        frame.setDim(this.value);
    }
}
