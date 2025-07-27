package com.mafuyu404.oneenoughitem.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mafuyu404.oneenoughitem.network.NetworkHandler;
import com.mafuyu404.oneenoughitem.network.ReplacementSyncPacket;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplacementDataManager implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<Replacements> replacements = new ArrayList<>();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Oneenoughitem.MODID, "replacement_data_manager");
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
        ReplacementCache.clearCache();

        HolderLookup.RegistryLookup<Item> itemRegistryLookup;
        if (currentServer != null) {
            itemRegistryLookup = currentServer.registryAccess().lookupOrThrow(Registries.ITEM);
            Oneenoughitem.LOGGER.debug("Using server registry access for tag resolution");
        } else {
            RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            itemRegistryLookup = access.lookupOrThrow(Registries.ITEM);
            Oneenoughitem.LOGGER.warn("Server not available, using built-in registry access. Some tags may not be resolved.");
        }

        Map<ResourceLocation, Resource> resources = resourceManager.listResources("replacements", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            if (!Oneenoughitem.MODID.equals(entry.getKey().getNamespace())) continue;

            try (InputStream inputStream = entry.getValue().open(); InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);

                if (jsonElement.isJsonArray()) {
                    for (var element : jsonElement.getAsJsonArray()) {
                        var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);
                        result.result().ifPresentOrElse(replacement -> {
                            List<Item> expanded = Utils.resolveItemList(replacement.matchItems(), itemRegistryLookup);
                            if (expanded.isEmpty()) {
                                Oneenoughitem.LOGGER.warn("No valid items resolved from matchItems in {}", entry.getKey());
                                return;
                            }
                            replacements.add(replacement);
                            ReplacementCache.putReplacement(replacement, itemRegistryLookup);
                            Oneenoughitem.LOGGER.info("Added replacement rule: {} -> {}",
                                    expanded.stream().map(Utils::getItemRegistryName).toList(),
                                    replacement.resultItems());
                        }, () -> Oneenoughitem.LOGGER.error("Failed to parse replacement data from {}: {}",
                                entry.getKey(), result.error().orElse(null)));
                    }
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Error loading replacement data from {}", entry.getKey(), e);
            }
        }

        Oneenoughitem.LOGGER.debug("Loaded {} replacement rules", replacements.size());
        Oneenoughitem.LOGGER.debug("Cache contents: {}", ReplacementCache.getCacheContents());
        syncToAllPlayers();
    }

    public static void syncToPlayer(ServerPlayer player) {
        Map<String, String> cacheContents = ReplacementCache.getCacheContents();
        NetworkHandler.sendToPlayer(player, new ReplacementSyncPacket(cacheContents));
        Oneenoughitem.LOGGER.debug("Synced replacement data to player: {}", player.getName().getString());
    }

    public static void syncToAllPlayers() {
        if (currentServer != null) {
            var allPlayers = PlayerLookup.all(currentServer);
            Map<String, String> cacheContents = ReplacementCache.getCacheContents();
            ReplacementSyncPacket packet = new ReplacementSyncPacket(cacheContents);
            for (ServerPlayer player : allPlayers) {
                NetworkHandler.sendToPlayer(player, packet);
            }
            Oneenoughitem.LOGGER.debug("Synced replacement data to {} players", allPlayers.size());
        } else {
            Oneenoughitem.LOGGER.warn("Server instance not available, cannot sync to all players");
        }
    }

    public static List<Replacements> getReplacements() {
        return replacements;
    }
}