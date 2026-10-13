package me.prexorjustin.serverlistplus.platform;

//? if neoforge {
/*import me.prexorjustin.serverlistplus.ServerListPlus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@Mod(value = ServerListPlus.MOD_ID, dist = Dist.CLIENT)
public final class ServerListPlusNeoForge {
    public ServerListPlusNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        ServerListPlus.init();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ServerListPlus.onJoin());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ServerListPlus.onLeave());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ServerListPlus.onTick());
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> ServerListPlus.onLeave());
    }
}
*///?}
