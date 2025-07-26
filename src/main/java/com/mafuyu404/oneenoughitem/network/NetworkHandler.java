package com.mafuyu404.oneenoughitem.network;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHandler {
    public static final ResourceLocation REPLACEMENT_SYNC_PACKET = ResourceLocation.fromNamespaceAndPath(Oneenoughitem.MODID, "replacement_sync");

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ReplacementSyncPacket.TYPE, ReplacementSyncPacket.STREAM_CODEC);
    }

    public static void sendToPlayer(ServerPlayer player, ReplacementSyncPacket packet) {
        try {
            ServerPlayNetworking.send(player, packet);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to send packet to player {}", player.getName().getString(), e);
        }
    }
}