package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.core.DataManager;
import com.mafuyu404.oelib.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // 当客户端加载世界时，重建替换缓存
        // 这确保了从服务器同步的数据能正确应用到缓存中
        if (event.getLevel().isClientSide()) {
            rebuildClientCache();
        }
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        // 当数据重载时，重建客户端缓存
        if (event.isDataType(Replacements.class)) {
            rebuildClientCache();
            Oneenoughitem.LOGGER.info("Client replacement cache rebuilt due to data reload: {} entries",
                    event.getLoadedCount());
        }
    }

    private static void rebuildClientCache() {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager != null) {
            ReplacementCache.clearCache();

            var replacements = manager.getDataList();
            for (Replacements replacement : replacements) {
                ReplacementCache.putReplacement(replacement);
            }

            Oneenoughitem.LOGGER.info("Rebuilt client replacement cache with {} rules", replacements.size());
        }
    }
}