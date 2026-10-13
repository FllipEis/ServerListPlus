package me.prexorjustin.serverlistplus.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import me.prexorjustin.serverlistplus.ServerListPlus;
import me.prexorjustin.serverlistplus.bridge.MultiplayerScreenHook;
import me.prexorjustin.serverlistplus.bridge.ServerListBridge;
import me.prexorjustin.serverlistplus.bridge.ServerView;
import me.prexorjustin.serverlistplus.core.model.DetailsMode;
import me.prexorjustin.serverlistplus.core.model.Divider;
import me.prexorjustin.serverlistplus.core.model.Folder;
import me.prexorjustin.serverlistplus.core.model.Store;
import me.prexorjustin.serverlistplus.core.model.TabLayout;
import me.prexorjustin.serverlistplus.core.model.ViewSettings;
import me.prexorjustin.serverlistplus.ui.DividerEntry;
import me.prexorjustin.serverlistplus.ui.HiddenSectionEntry;
import me.prexorjustin.serverlistplus.ui.IconButton;
import me.prexorjustin.serverlistplus.ui.TabSidebar;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.List;

/**
 * Starts the real game and goes through the screens ServerListPlus changes. Each step checks
 * the result and saves a screenshot to {@code build/run/clientGameTest/screenshots}.
 */
public final class ServerListScreensTest implements FabricClientGameTest {
    /** Nothing listens there, so pings fail fast and the test needs no network. */
    private static final String SHARED_ADDRESS = "127.0.0.1:1";

    @Override
    public void runTest(ClientGameTestContext context) {
        if (ReadmeScreenshots.ENABLED) return;
        context.runOnClient(ServerListScreensTest::writeServerList);
        openServerList(context);
        context.takeScreenshot("01-server-list");

        sameAddressKeepsSeparateFolders(context);
        hiddenServersFoldAway(context);
        menusOpenOnClick(context);
        sidebarLayout(context);
        detailsModes(context);
        keyboardMoveCrossesDividers(context);
        miniMessageNames(context);
        directConnection(context);

        // The test framework expects to end where it started
        context.setScreen(TitleScreen::new);
        context.waitForScreen(TitleScreen.class);
    }

    private static void writeServerList(Minecraft minecraft) {
        ServerList servers = new ServerList(minecraft);
        servers.add(new ServerData("Space Chunks", SHARED_ADDRESS, ServerData.Type.OTHER), false);
        servers.add(new ServerData("BlockBound", SHARED_ADDRESS, ServerData.Type.OTHER), false);
        servers.add(new ServerData("Unreachable", "127.0.0.1:2", ServerData.Type.OTHER), false);
        servers.save();
    }

    private static void openServerList(ClientGameTestContext context) {
        context.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
        context.waitForScreen(JoinMultiplayerScreen.class);
        context.waitTicks(10);
    }

    /** Two entries with the same address are separate servers: each has its own folders. */
    private static void sameAddressKeepsSeparateFolders(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            Store store = ServerListPlus.store();
            Folder folder = store.addFolder(new Folder("Space Chunks only"));
            ServerView.metaOrCreate(server(minecraft, "Space Chunks")).addToFolder(folder.id());
            store.view().setTab(folder.id());
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(5);
        List<String> shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(shown.equals(List.of("Space Chunks")), "folder should only show Space Chunks, but shows " + shown);
        context.takeScreenshot("02-folder-with-one-of-two-same-address-servers");
    }

    /** A server hidden from "All" leaves the list and goes into the folded section at the end. */
    private static void hiddenServersFoldAway(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            ServerView.metaOrCreate(server(minecraft, "BlockBound")).setHiddenInAll(true);
            ServerListPlus.store().view().setTab(ViewSettings.TAB_ALL);
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(5);
        List<String> shown = context.computeOnClient(ServerListScreensTest::shownNames);
        check(!shown.contains("BlockBound"), "BlockBound is hidden but still shown: " + shown);
        boolean section = context.computeOnClient(minecraft -> list(minecraft).children().stream()
            .anyMatch(entry -> entry instanceof HiddenSectionEntry));
        check(section, "the hidden section header is missing");
        context.takeScreenshot("03-hidden-section");
    }

    /** Real clicks, so the input routing of the screen is tested too, not just the data. */
    private static void menusOpenOnClick(ClientGameTestContext context) {
        double[] server = context.computeOnClient(minecraft -> {
            ServerSelectionList list = list(minecraft);
            ServerSelectionList.Entry first = list.children().getFirst();
            int[] row = ServerListBridge.rowBounds(list, first);
            return new double[]{list.getX() + list.getWidth() / 2.0, (row[0] + row[1]) / 2.0};
        });
        click(context, server[0], server[1], InputConstants.MOUSE_BUTTON_RIGHT);
        context.takeScreenshot("04-server-menu");
        click(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT); // closes the menu

        double[] settings = context.computeOnClient(minecraft -> {
            IconButton button = minecraft.gui.screen().children().stream()
                .filter(child -> child instanceof IconButton)
                .map(child -> (IconButton) child)
                .reduce((first, second) -> second) // settings is the last of the three
                .orElseThrow(() -> new AssertionError("no header icons"));
            return new double[]{button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0};
        });
        click(context, settings[0], settings[1], InputConstants.MOUSE_BUTTON_LEFT);
        context.takeScreenshot("05-settings-menu");
        click(context, 4, 4, InputConstants.MOUSE_BUTTON_LEFT);
    }

    /** Clicks at a position in GUI coordinates. */
    private static void click(ClientGameTestContext context, double x, double y, int button) {
        double scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
        context.getInput().setCursorPos(x * scale, y * scale);
        context.getInput().pressMouse(button);
        context.waitTicks(2);
    }

    /** The sidebar needs room next to the list, so this step uses a wider window. */
    private static void sidebarLayout(ClientGameTestContext context) {
        context.getInput().resizeWindow(1600, 900);
        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.SIDEBAR));
        // The layout is applied when the screen is laid out again
        openServerList(context);
        boolean sidebar = context.computeOnClient(minecraft -> minecraft.gui.screen().children().stream()
            .anyMatch(child -> child instanceof TabSidebar sidebarWidget && sidebarWidget.visible));
        check(sidebar, "the sidebar is not shown in a wide window");
        context.takeScreenshot("06-sidebar");
        context.runOnClient(minecraft -> ServerListPlus.store().view().setTabLayout(TabLayout.TOP));
        context.getInput().resizeWindow(854, 480);
        openServerList(context);
    }

    private static void detailsModes(ClientGameTestContext context) {
        for (DetailsMode mode : DetailsMode.values()) {
            context.runOnClient(minecraft -> {
                ServerListPlus.store().activityOrCreate(SHARED_ADDRESS).setLastPlayed(System.currentTimeMillis() - 3_600_000);
                ServerListPlus.store().view().setDetails(mode);
            });
            context.waitTicks(2);
            context.takeScreenshot("07-details-" + mode.name().toLowerCase());
        }
    }

    /**
     * Shift + arrow key moves a server across a divider instead of stopping in front of it. Real key
     * presses in a row, so the focus has to follow the server for the next press to arrive.
     */
    private static void keyboardMoveCrossesDividers(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            ServerListPlus.store().dividers().add(new Divider(ViewSettings.TAB_ALL, ServerView.key(server(minecraft, "Unreachable"))));
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(2);
        checkRows(context, "Space Chunks", "---", "Unreachable");

        clickServer(context, "Space Chunks");
        pressShifted(context, InputConstants.KEY_DOWN);
        checkRows(context, "---", "Space Chunks", "Unreachable");
        pressShifted(context, InputConstants.KEY_DOWN);
        checkRows(context, "---", "Unreachable", "Space Chunks");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "---", "Space Chunks", "Unreachable");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Space Chunks", "---", "Unreachable");

        clickServer(context, "Unreachable");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Space Chunks", "Unreachable", "---");
        pressShifted(context, InputConstants.KEY_UP);
        checkRows(context, "Unreachable", "Space Chunks", "---");
        context.takeScreenshot("08-moved-by-keyboard");
    }

    private static void clickServer(ClientGameTestContext context, String name) {
        double[] center = context.computeOnClient(minecraft -> {
            ServerData data = server(minecraft, name);
            ServerSelectionList list = list(minecraft);
            ServerSelectionList.Entry entry = list.children().stream()
                .filter(child -> child instanceof ServerSelectionList.OnlineServerEntry online && online.getServerData() == data)
                .findFirst().orElseThrow();
            int[] row = ServerListBridge.rowBounds(list, entry);
            return new double[]{list.getX() + list.getWidth() / 2.0, (row[0] + row[1]) / 2.0};
        });
        click(context, center[0], center[1], InputConstants.MOUSE_BUTTON_LEFT);
    }

    /**
     * Shift + key through the screen, so it travels the same focus path as a real key press. The
     * test input cannot be used here: it always sends key events without modifiers.
     */
    private static void pressShifted(ClientGameTestContext context, int key) {
        context.runOnClient(minecraft -> minecraft.gui.screen().keyPressed(new KeyEvent(key, 0, InputConstants.MOD_SHIFT)));
        context.waitTicks(2);
    }

    /** Servers and dividers ("---") in the order shown, without the hidden section. */
    private static void checkRows(ClientGameTestContext context, String... expected) {
        List<String> rows = context.computeOnClient(minecraft -> list(minecraft).children().stream()
            .takeWhile(entry -> !(entry instanceof HiddenSectionEntry))
            .map(entry -> entry instanceof DividerEntry ? "---"
                : entry instanceof ServerSelectionList.OnlineServerEntry online ? online.getServerData().name : null)
            .filter(row -> row != null)
            .toList());
        check(rows.equals(List.of(expected)), "expected rows " + List.of(expected) + ", but got " + rows);
    }

    /** Names with MiniMessage tags are drawn formatted instead of as typed. */
    private static void miniMessageNames(ClientGameTestContext context) {
        context.runOnClient(minecraft -> {
            server(minecraft, "Space Chunks").name = "<gradient:#ff5555:#ffaa00>Space Chunks</gradient> <gray>SMP";
            hook(minecraft).serverlistplus$refresh();
        });
        context.waitTicks(2);
        context.takeScreenshot("09-minimessage-name");
    }

    /** The preview pings while the address is typed; drawing it before the ping started once crashed. */
    private static void directConnection(ClientGameTestContext context) {
        context.runOnClient(minecraft -> minecraft.gui.setScreen(new DirectJoinServerScreen(minecraft.gui.screen(),
            joined -> { }, new ServerData("", "", ServerData.Type.OTHER))));
        context.waitForScreen(DirectJoinServerScreen.class);
        context.getInput().typeChars("127.0.0.1:3");
        context.waitTicks(30);
        context.takeScreenshot("10-direct-connection");
    }

    private static ServerData server(Minecraft minecraft, String name) {
        ServerList servers = hook(minecraft).serverlistplus$servers();
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i).name.equals(name)) return servers.get(i);
        }
        throw new AssertionError("no server named " + name);
    }

    private static List<String> shownNames(Minecraft minecraft) {
        return list(minecraft).children().stream()
            .filter(entry -> entry instanceof ServerSelectionList.OnlineServerEntry)
            .map(entry -> ((ServerSelectionList.OnlineServerEntry) entry).getServerData().name)
            .toList();
    }

    private static ServerSelectionList list(Minecraft minecraft) {
        return hook(minecraft).serverlistplus$list();
    }

    private static MultiplayerScreenHook hook(Minecraft minecraft) {
        return (MultiplayerScreenHook) minecraft.gui.screen();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
