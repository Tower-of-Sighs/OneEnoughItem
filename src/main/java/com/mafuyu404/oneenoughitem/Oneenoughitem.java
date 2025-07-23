package com.mafuyu404.oneenoughitem;

import com.mafuyu404.oneenoughitem.event.ServerEventHandler;
import com.mafuyu404.oneenoughitem.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Oneenoughitem implements ModInitializer {
    public static final String MODID = "oneenoughitem";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        ServerEventHandler.register();
        NetworkHandler.register();
    }
}