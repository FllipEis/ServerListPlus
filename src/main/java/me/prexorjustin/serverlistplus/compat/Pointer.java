package me.prexorjustin.serverlistplus.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * A mouse button event, independent of how the current version delivers it.
 */
public record Pointer(double x, double y, int button) {
    public boolean isLeft() {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    public boolean isRight() {
        return button == InputConstants.MOUSE_BUTTON_RIGHT;
    }
}
