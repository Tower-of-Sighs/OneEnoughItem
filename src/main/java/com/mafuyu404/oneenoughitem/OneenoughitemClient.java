package com.mafuyu404.oneenoughitem;

import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.event.ClientEventHandler;
import net.fabricmc.api.ClientModInitializer;

public class OneenoughitemClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModKeyMappings.register();
        ClientEventHandler.register();
    }
}