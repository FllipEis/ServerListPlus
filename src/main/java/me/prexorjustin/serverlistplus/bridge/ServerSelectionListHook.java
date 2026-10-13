package me.prexorjustin.serverlistplus.bridge;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

/**
 * Implemented by {@code ServerSelectionList} through a mixin.
 */
public interface ServerSelectionListHook {
    JoinMultiplayerScreen serverlistplus$screen();

    /** Scrolls just far enough to show the row. */
    void serverlistplus$scrollTo(ServerSelectionList.Entry entry);
}
