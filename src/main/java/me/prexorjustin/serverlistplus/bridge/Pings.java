package me.prexorjustin.serverlistplus.bridge;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
//? if >=1.21.11
import net.minecraft.server.network.EventLoopGroupHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server status pings started by ServerListPlus, off the render thread like vanilla's own.
 */
public final class Pings {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "ServerListPlus status ping");
        thread.setDaemon(true);
        return thread;
    });

    private Pings() {
    }

    /** A ping in progress. The pinger writes its results into {@link #data} while it runs. */
    public static final class Ping {
        private final ServerData data;
        private final long started = System.currentTimeMillis();
        private volatile boolean answered;
        private volatile boolean failed;

        private Ping(ServerData data) {
            this.data = data;
        }

        public ServerData data() {
            return data;
        }

        /** The server answered with its status and ping time. */
        public boolean answered() {
            return answered;
        }

        /** The address could not be resolved or the connection failed right away. */
        public boolean failed() {
            return failed;
        }

        public long ageMillis() {
            return System.currentTimeMillis() - started;
        }
    }

    /**
     * Pings {@code data}; the pinger must be ticked until the ping is done and should be
     * cleared with {@link ServerStatusPinger#removeAll()} when it is no longer needed.
     */
    public static Ping start(ServerStatusPinger pinger, ServerData data) {
        Ping ping = new Ping(data);
        POOL.submit(() -> {
            try {
                //? if >=1.21.11 {
                pinger.pingServer(data, () -> { }, () -> ping.answered = true,
                    EventLoopGroupHolder.remote(Minecraft.getInstance().options.useNativeTransport()));
                //?} else
                /*pinger.pingServer(data, () -> { }, () -> ping.answered = true);*/
            } catch (Exception e) {
                ping.failed = true;
            }
        });
        return ping;
    }

    public static int currentProtocol() {
        //? if >=1.21.6 {
        return SharedConstants.getCurrentVersion().protocolVersion();
        //?} else
        /*return SharedConstants.getCurrentVersion().getProtocolVersion();*/
    }

    public static boolean isCompatible(ServerData data) {
        return data.state() != ServerData.State.INCOMPATIBLE && data.protocol == currentProtocol();
    }
}
