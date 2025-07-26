package com.mafuyu404.oneenoughitem.network;

import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Cache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record ReplacementSyncPacket(List<Replacements> replacements) implements CustomPacketPayload {

    public static final Type<ReplacementSyncPacket> TYPE = new Type<>(NetworkHandler.REPLACEMENT_SYNC_PACKET);

    public static final StreamCodec<RegistryFriendlyByteBuf, ReplacementSyncPacket> STREAM_CODEC = StreamCodec.of(
            ReplacementSyncPacket::encode,
            ReplacementSyncPacket::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, ReplacementSyncPacket packet) {
        buf.writeInt(packet.replacements.size());
        for (Replacements replacement : packet.replacements) {
            buf.writeInt(replacement.matchItems().size());
            for (String target : replacement.matchItems()) {
                buf.writeUtf(target);
            }
            buf.writeUtf(replacement.resultItems());
        }
    }

    public static ReplacementSyncPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Replacements> replacements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int targetSize = buf.readInt();
            List<String> targets = new ArrayList<>();
            for (int j = 0; j < targetSize; j++) {
                targets.add(buf.readUtf());
            }
            String replace = buf.readUtf();
            replacements.add(new Replacements(targets, replace));
        }
        return new ReplacementSyncPacket(replacements);
    }

    public void handleClient() {
        Cache.clearCache();
        for (Replacements replacement : this.replacements) {
            Cache.putReplacement(replacement);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}