package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.forge.data.DataManager;
import com.mafuyu404.oelib.forge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;

@Mod.EventBusSubscriber(modid = "oneenoughitem")
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        rebuildReplacementCache("server-start");
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            rebuildReplacementCache("data-reload");
            Oneenoughitem.LOGGER.info("Server replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    event.getLoadedCount(), event.getInvalidCount());
            Oneenoughitem.LOGGER.info("Recipe JSON rewrite mode (server): {}", String.valueOf(Config.DATA_REWRITE_MODE.get()));

            ReplacementCache.endReloadOverride();
        }
    }

    private static void rebuildReplacementCache(String reason) {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager != null) {
            ReplacementCache.clearCache();

            var replacements = manager.getDataList();
            for (Replacements replacement : replacements) {
                ReplacementCache.putReplacement(replacement);
            }

            Oneenoughitem.LOGGER.debug("Server rebuilt replacement cache (reason: {}) with {} rules", reason, replacements.size());
        } else {
            Oneenoughitem.LOGGER.warn("ServerEventHandler: No replacement data manager found in OELib (reason: {})", reason);
        }
    }
}