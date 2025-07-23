package com.mafuyu404.oneenoughitem.network;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {
    public static final ResourceLocation REPLACEMENT_SYNC_PACKET = new ResourceLocation(Oneenoughitem.MODID, "replacement_sync");

    public static void register() {

    }

    public static void sendToPlayer(ServerPlayer player, ReplacementSyncPacket packet) {
        try {
            ServerPlayNetworking.send(player, REPLACEMENT_SYNC_PACKET, packet.toPacketByteBuf());
        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to send packet to player {}", player.getName().getString(), e);
        }
    }
}