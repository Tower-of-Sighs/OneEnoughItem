package com.mafuyu404.oneenoughitem.client;

import com.mafuyu404.oneenoughitem.network.NetworkHandler;
import com.mafuyu404.oneenoughitem.network.ReplacementSyncPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OneenoughitemClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.REPLACEMENT_SYNC_PACKET,
                (client, handler, buf, responseSender) -> {
                    ReplacementSyncPacket packet = ReplacementSyncPacket.decode(buf);
                    client.execute(packet::handleClient);
                });
    }
}