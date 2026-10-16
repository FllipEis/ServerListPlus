package me.prexorjustin.serverlistplus.ui.widget;

import me.prexorjustin.serverlistplus.compat.Gfx;
import me.prexorjustin.serverlistplus.compat.Pointer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Base for ServerListPlus widgets: translates the version-specific render and mouse methods
 * into {@link Gfx} and {@link Pointer}, and accepts every mouse button.
 */
public abstract class SlpWidget extends AbstractWidget {
    private static final long DOUBLE_CLICK_MILLIS = 400;

    private long lastPress;
    private double lastPressX;
    private double lastPressY;

    protected SlpWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    protected abstract void draw(Gfx gfx, int mouseX, int mouseY);

    protected boolean press(Pointer pointer, boolean doubleClick) {
        return false;
    }

    protected boolean release(Pointer pointer) {
        return false;
    }

    private boolean dispatchPress(Pointer pointer, boolean gameDoubleClick) {
        if (!this.active || !this.visible || !this.isMouseOver(pointer.x(), pointer.y())) return false;
        long now = System.currentTimeMillis();
        boolean doubleClick = gameDoubleClick || now - lastPress < DOUBLE_CLICK_MILLIS
            && Math.abs(pointer.x() - lastPressX) < 4 && Math.abs(pointer.y() - lastPressY) < 4;
        lastPress = doubleClick ? 0 : now;
        lastPressX = pointer.x();
        lastPressY = pointer.y();
        return press(pointer, doubleClick);
    }

    //? if >=26.1 {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        draw(new Gfx(graphics), mouseX, mouseY);
    }
    //?} else {
    /*@Override
    protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Gfx(graphics), mouseX, mouseY);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return dispatchPress(new Pointer(event.x(), event.y(), event.button()), doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return release(new Pointer(event.x(), event.y(), event.button()));
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return dispatchPress(new Pointer(mouseX, mouseY, button), false);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return release(new Pointer(mouseX, mouseY, button));
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
