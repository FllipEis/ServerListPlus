package me.prexorjustin.serverlistplus.mixin;

import me.prexorjustin.serverlistplus.ui.DirectJoinUi;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DirectJoinServerScreen.class)
public abstract class DirectJoinServerScreenMixin extends Screen {
    @Shadow
    private EditBox ipEdit;
    @Shadow
    private Button selectButton;
    @Shadow
    @Final
    private Screen lastScreen;

    @Shadow
    private void onSelect() {
        throw new AssertionError();
    }

    @Unique
    private DirectJoinUi serverlistplus$ui;

    protected DirectJoinServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void serverlistplus$layout(CallbackInfo ci) {
        if (serverlistplus$ui == null) {
            serverlistplus$ui = new DirectJoinUi(this.font, lastScreen, value -> ipEdit.setValue(value), () -> {
                if (selectButton.active) onSelect();
            });
        }
        Button cancel = null;
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button && button != selectButton) cancel = button;
        }
        for (AbstractWidget widget : serverlistplus$ui.layout(this.width, this.height, ipEdit, selectButton, cancel)) {
            this.addRenderableWidget(widget);
        }
        serverlistplus$ui.addressChanged(ipEdit.getValue());
    }

    @Inject(method = "updateSelectButtonStatus", at = @At("TAIL"))
    private void serverlistplus$addressChanged(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.addressChanged(ipEdit.getValue());
    }

    @Inject(method = "onSelect", at = @At("HEAD"))
    private void serverlistplus$remember(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.joining(ipEdit.getValue());
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void serverlistplus$close(CallbackInfo ci) {
        if (serverlistplus$ui != null) serverlistplus$ui.close();
    }

    @Override
    public void tick() {
        super.tick();
        if (serverlistplus$ui != null) serverlistplus$ui.tick(ipEdit.getValue());
    }

    /** Vanilla draws the label at a fixed height; move it above the relocated input field. */
    //? if >=26.1 {
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 100, ordinal = 1))
    //?} else
    /*@ModifyConstant(method = "render", constant = @Constant(intValue = 100, ordinal = 1))*/
    private int serverlistplus$labelY(int y) {
        return DirectJoinUi.labelY();
    }
}
