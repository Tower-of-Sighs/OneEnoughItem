package com.mafuyu404.oneenoughitem.network;

import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;

public record ReplacementSyncPacket(Map<String, String> mappings) implements CustomPacketPayload {

    public static final Type<ReplacementSyncPacket> TYPE = new Type<>(NetworkHandler.REPLACEMENT_SYNC_PACKET);

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplacementSyncPacket> STREAM_CODEC = StreamCodec.of(
            ReplacementSyncPacket::encode,
            ReplacementSyncPacket::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, ReplacementSyncPacket packet) {
        buf.writeInt(packet.mappings.size());
        for (Map.Entry<String, String> entry : packet.mappings.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static ReplacementSyncPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> mappings = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String sourceId = buf.readUtf();
            String targetId = buf.readUtf();
            mappings.put(sourceId, targetId);
        }
        return new ReplacementSyncPacket(mappings);
    }

    public void handleClient() {
        ReplacementCache.clearCache();
        ReplacementCache.putReplacementsBatch(this.mappings);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}