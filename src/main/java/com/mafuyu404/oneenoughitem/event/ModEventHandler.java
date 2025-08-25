package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mafuyu404.oelib.neoforge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ModConfig;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID)
public class ModEventHandler {
    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            rebuildReplacementCache("data-reload");
            Oneenoughitem.LOGGER.info("Server replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    event.getLoadedCount(), event.getInvalidCount());
            Oneenoughitem.LOGGER.info("Recipe JSON rewrite mode (server): {}", ModConfig.DATA_REWRITE_MODE.getValue());

            ReplacementCache.endReloadOverride();
        }
    }

    private static void rebuildReplacementCache(String reason) {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager == null) {
            Oneenoughitem.LOGGER.warn("ServerEventHandler: No replacement data manager found in OELib (reason: {})", reason);
            return;
        }

        ReplacementCache.clearCache();
        var server = DataManager.getCurrentServer();
        HolderLookup.RegistryLookup<Item> registryLookup = null;
        boolean hasServer = false;

        if (server != null) {
            registryLookup = server.registryAccess().lookupOrThrow(Registries.ITEM);
            hasServer = true;
        }

        var replacements = manager.getDataList();
        for (Replacements replacement : replacements) {
            ReplacementCache.putReplacement(replacement, registryLookup);
        }

        Oneenoughitem.LOGGER.debug(
                "Server rebuilt replacement cache (reason: {}, mode: {}, rules: {})",
                reason,
                hasServer ? "with registry" : "provisional",
                replacements.size()
        );
    }

}
