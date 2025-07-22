package com.mafuyu404.oneenoughitem.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Cache;
import com.mafuyu404.oneenoughitem.network.NetworkHandler;
import com.mafuyu404.oneenoughitem.network.ReplacementSyncPacket;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplacementDataManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<Replacements> replacements = new ArrayList<>();

    public ReplacementDataManager() {
        super(GSON, "replacements");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        replacements.clear();
        Cache.clearCache();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            try {
                if (entry.getValue().isJsonArray()) {
                    var array = entry.getValue().getAsJsonArray();
                    for (var element : array) {
                        var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            Replacements replacement = result.result().get();
                            replacements.add(replacement);
                            Cache.putReplacement(replacement);
                        } else {
                            Oneenoughitem.LOGGER.error("Failed to parse replacement data from {}: {}",
                                    entry.getKey(), result.error().orElse(null));
                        }
                    }
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Error loading replacement data from {}", entry.getKey(), e);
            }
        }

        Oneenoughitem.LOGGER.info("Loaded {} replacement rules", replacements.size());

        // 数据重新加载后，同步到所有在线玩家
        syncToAllPlayersIfServerRunning();
    }

    private static void syncToAllPlayersIfServerRunning() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            Oneenoughitem.LOGGER.info("Syncing replacement data to {} online players",
                    server.getPlayerList().getPlayerCount());
            syncToAllPlayers();
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new ReplacementSyncPacket(replacements));
    }

    public static void syncToAllPlayers() {
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new ReplacementSyncPacket(replacements));
    }

    public static List<Replacements> getReplacements() {
        return replacements;
    }//预留以后优化
}