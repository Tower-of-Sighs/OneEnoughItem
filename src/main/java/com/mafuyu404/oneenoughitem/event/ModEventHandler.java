package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.core.DataManager;
import com.mafuyu404.oelib.event.DataReloadEvent;
import com.mafuyu404.oelib.event.Events;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;

public class ModEventHandler {

    public static void register() {
        Events.on(DataReloadEvent.EVENT)
                .normal()
                .register(ModEventHandler::onDataReload);
    }

    public static void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount) {
        if (dataClass == Replacements.class) {
            rebuildReplacementCache();
            Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    loadedCount, invalidCount);
        }
    }

    private static void rebuildReplacementCache() {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager != null) {
            ReplacementCache.clearCache();

            MinecraftServer server = manager.getCurrentServer();
            if (server != null) {
                HolderLookup.RegistryLookup<Item> registryLookup = server.registryAccess().lookupOrThrow(Registries.ITEM);

                var replacements = manager.getDataList();
                for (Replacements replacement : replacements) {
                    ReplacementCache.putReplacement(replacement, registryLookup);
                }

                Oneenoughitem.LOGGER.debug("Rebuilt replacement cache with {} rules from OELib data manager",
                        replacements.size());
            } else {
                Oneenoughitem.LOGGER.warn("No server instance available, cannot rebuild replacement cache with registry lookup");

                var replacements = manager.getDataList();
                for (Replacements replacement : replacements) {
                    Oneenoughitem.LOGGER.debug("Skipping replacement cache for {} due to missing server context",
                            replacement.resultItems());
                }
            }
        } else {
            Oneenoughitem.LOGGER.warn("No replacement data manager found in OELib");
        }
    }
}