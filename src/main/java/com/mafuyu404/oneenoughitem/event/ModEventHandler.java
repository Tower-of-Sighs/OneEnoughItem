package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mafuyu404.oelib.neoforge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.config.ModConfig;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID)
public class ModEventHandler {
    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            rebuildReplacementCache("data-reload");
            Oneenoughitem.LOGGER.info("Server replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    event.getLoadedCount(), event.getInvalidCount());
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
            Replacements toPut = getReplacements(replacement);
            ReplacementCache.putReplacement(toPut, registryLookup);
        }

        Oneenoughitem.LOGGER.debug(
                "Server rebuilt replacement cache (reason: {}, mode: {}, rules: {})",
                reason,
                hasServer ? "with registry" : "provisional",
                replacements.size()
        );
    }

    private static @NotNull Replacements getReplacements(Replacements replacement) {
        Replacements toPut = replacement;
        if (replacement.rules().isEmpty()) {
            var cfg = ModConfig.DEFAULT_RULES.getValue();
            if (cfg != null) {
                toPut = new Replacements(
                        replacement.matchItems(),
                        replacement.resultItems(),
                        Optional.of(cfg.toRules())
                );
            }
        }
        return toPut;
    }
}
