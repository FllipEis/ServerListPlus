package me.prexorjustin.serverlistplus.platform;

//? if fabric {
import me.prexorjustin.serverlistplus.ServerListPlus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ServerListPlusFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ServerListPlus.init();
        // Run on the client thread, whatever thread the event fires on
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(ServerListPlus::onJoin));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ServerListPlus::onLeave));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ServerListPlus.onTick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ServerListPlus.onLeave());
    }
}
//?}
