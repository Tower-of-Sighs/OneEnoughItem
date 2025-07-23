package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oneenoughitem.data.ReplacementDataManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

public class ServerEventHandler {
    private static final ReplacementDataManager DATA_MANAGER = new ReplacementDataManager();

    //Fuck fabric api
    public static void register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(DATA_MANAGER);

        ServerLifecycleEvents.SERVER_STARTED.register(ReplacementDataManager::setServer);

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ReplacementDataManager.setServer(null);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ReplacementDataManager.syncToPlayer(player);
        });
    }
}