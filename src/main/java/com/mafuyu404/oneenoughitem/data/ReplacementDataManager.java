package com.mafuyu404.oneenoughitem.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Cache;
import com.mafuyu404.oneenoughitem.init.Utils;
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

        Oneenoughitem.LOGGER.info("Starting to load replacement data from {} files", object.size());

        int validReplacements = 0;
        int invalidReplacements = 0;


        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();

            if (!Oneenoughitem.MODID.equals(fileLocation.getNamespace())) {
                continue;
            }

            Oneenoughitem.LOGGER.debug("Processing replacement file: {}", entry.getKey());

            try {
                if (entry.getValue().isJsonArray()) {
                    var array = entry.getValue().getAsJsonArray();
                    for (var element : array) {
                        var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            Replacements replacement = result.result().get();

                            if (validateReplacement(replacement, entry.getKey())) {
                                replacements.add(replacement);
                                Cache.putReplacement(replacement);
                                validReplacements++;
                            } else {
                                invalidReplacements++;
                                Oneenoughitem.LOGGER.warn("Skipped invalid replacement: {} -> {}",
                                        replacement.matchItems(), replacement.resultItems());
                            }
                        } else {
                            Oneenoughitem.LOGGER.error("Failed to parse replacement data from {}: {}",
                                    entry.getKey(), result.error().orElse(null));
                            invalidReplacements++;
                        }
                    }
                } else {
                    Oneenoughitem.LOGGER.warn("Replacement file {} does not contain a JSON array, skipping", entry.getKey());
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Error loading replacement data from {}", entry.getKey(), e);
                invalidReplacements++;
            }
        }

        Oneenoughitem.LOGGER.info("Loaded {} valid replacement rules, {} invalid rules were skipped",
                validReplacements, invalidReplacements);

        syncToAllPlayersIfServerRunning();
    }

    private boolean  validateReplacement(Replacements replacement, ResourceLocation sourceFile) {

        if (Utils.getItemById(replacement.resultItems()) == null) {
            Oneenoughitem.LOGGER.error("Invalid replacement in {}: target item '{}' does not exist",
                    sourceFile, replacement.resultItems());
            return false;
        }

        boolean hasValidSource = false;

        for (String matchItem : replacement.matchItems()) {
            if (Utils.getItemById(matchItem) != null) {
                hasValidSource = true;
            } else {
                Oneenoughitem.LOGGER.warn("Invalid source item in {}: '{}' does not exist",
                        sourceFile, matchItem);
            }
        }

        if (!hasValidSource) {
            Oneenoughitem.LOGGER.error("Invalid replacement in {}: no valid source items found for target '{}'",
                    sourceFile, replacement.resultItems());
            return false;
        }

        return true;
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