package com.mafuyu404.oneenoughitem.network;

import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public class ReplacementSyncPacket {
    private final List<Replacements> replacements;

    public ReplacementSyncPacket(List<Replacements> replacements) {
        this.replacements = replacements;
    }

    public FriendlyByteBuf toPacketByteBuf() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        encode(this, buf);
        return buf;
    }

    public static void encode(ReplacementSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.replacements.size());
        for (Replacements replacement : packet.replacements) {
            buf.writeInt(replacement.matchItems().size());
            for (String target : replacement.matchItems()) {
                buf.writeUtf(target);
            }
            buf.writeUtf(replacement.resultItems());
        }
    }

    public static ReplacementSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Replacements> replacements = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            int targetSize = buf.readInt();
            List<String> targets = new java.util.ArrayList<>();
            for (int j = 0; j < targetSize; j++) {
                targets.add(buf.readUtf());
            }
            String replace = buf.readUtf();
            replacements.add(new Replacements(targets, replace));
        }
        return new ReplacementSyncPacket(replacements);
    }

    public void handleClient() {
        ReplacementCache.clearCache();
        for (Replacements replacement : this.replacements) {
            ReplacementCache.putReplacement(replacement);
        }
    }
}