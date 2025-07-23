package com.mafuyu404.oneenoughitem.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Cache;
import com.mafuyu404.oneenoughitem.network.NetworkHandler;
import com.mafuyu404.oneenoughitem.network.ReplacementSyncPacket;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplacementDataManager implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<Replacements> replacements = new ArrayList<>();
    private static final ResourceLocation ID = new ResourceLocation(Oneenoughitem.MODID, "replacement_data_manager");
    private static MinecraftServer currentServer = null;

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }

    public static void setServer(MinecraftServer server) {
        currentServer = server;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        replacements.clear();
        Cache.clearCache();

        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources =
                resourceManager.listResources("replacements", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> entry : resources.entrySet()) {
            try (InputStream inputStream = entry.getValue().open();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {

                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);

                if (jsonElement.isJsonArray()) {
                    var array = jsonElement.getAsJsonArray();
                    for (var element : array) {
                        var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            Replacements replacement = result.result().get();
                            replacements.add(replacement);
                            Cache.putReplacement(replacement);
                            Oneenoughitem.LOGGER.info("Added replacement rule: {} -> {}",
                                    replacement.matchItems(), replacement.resultItems());
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

        Oneenoughitem.LOGGER.debug("Loaded {} replacement rules", replacements.size());

        Oneenoughitem.LOGGER.debug("Cache contents: {}", Cache.getCacheContents());
        syncToAllPlayers();
    }

    public static void syncToPlayer(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new ReplacementSyncPacket(replacements));
        Oneenoughitem.LOGGER.debug("Synced replacement data to player: {}", player.getName().getString());
    }

    public static void syncToAllPlayers() {
        if (currentServer != null) {
            try {
                // Fuck fabric api
                var allPlayers = PlayerLookup.all(currentServer);
                ReplacementSyncPacket packet = new ReplacementSyncPacket(replacements);
                for (ServerPlayer player : allPlayers) {
                    NetworkHandler.sendToPlayer(player, packet);
                }
                Oneenoughitem.LOGGER.debug("Synced replacement data to {} players", allPlayers.size());
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Failed to sync replacement data to all players", e);
            }
        } else {
            Oneenoughitem.LOGGER.warn("Server instance not available, cannot sync to all players");
        }
    }

    public static List<Replacements> getReplacements() {
        return replacements;
    }
}