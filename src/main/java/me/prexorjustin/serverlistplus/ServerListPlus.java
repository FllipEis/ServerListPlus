package me.prexorjustin.serverlistplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerListPlus implements ModInitializer {

    public static final String MOD_ID = "serverlistplus";
    public static final String MOD_NAME = "ServerListPlus";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info(MOD_NAME + " initialized.");
    }

}
