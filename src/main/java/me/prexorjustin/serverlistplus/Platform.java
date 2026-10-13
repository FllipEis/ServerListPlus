package me.prexorjustin.serverlistplus;

import java.nio.file.Path;

/**
 * Loader-specific functionality behind a common interface.
 */
public sealed interface Platform {
    Platform INSTANCE =
        /*? if fabric {*/new FabricPlatform();
        /*?} elif neoforge *///new NeoForgePlatform();

    String loaderName();

    Path configDir();

    boolean isModLoaded(String id);

    //? if fabric {
    final class FabricPlatform implements Platform {
        private final net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();

        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDir() {
            return loader.getConfigDir();
        }

        @Override
        public boolean isModLoaded(String id) {
            return loader.isModLoaded(id);
        }
    }
    //?} elif neoforge {
    /*final class NeoForgePlatform implements Platform {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public Path configDir() {
            return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        }

        @Override
        public boolean isModLoaded(String id) {
            return net.neoforged.fml.ModList.get().isLoaded(id);
        }
    }
    *///?}
}
