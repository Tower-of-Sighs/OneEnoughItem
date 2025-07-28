package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 前置会自动同步数据到客户端
            // 我们只需要确保服务器端缓存是最新的
            if (ReplacementCache.getCacheSize() == 0) {
                ReplacementEventHandler.rebuildReplacementCache();
            }

            Oneenoughitem.LOGGER.debug("Player {} logged in, replacement cache size: {}",
                    serverPlayer.getName().getString(), ReplacementCache.getCacheSize());
        }
    }
}