package me.prexorjustin.serverlistplus.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListInvoker {
    @Invoker("getRowTop")
    int serverlistplus$rowTop(int index);

    @Invoker("getRowBottom")
    int serverlistplus$rowBottom(int index);
}
