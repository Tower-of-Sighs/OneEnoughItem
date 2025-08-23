package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mafuyu404.oelib.neoforge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.util.ModernFixDetector;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID)
public class ModEventHandler {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (ModernFixDetector.shouldShowWarning()) {
            ModernFixDetector.markWarningShown();

            MutableComponent line1 = Component.translatable("oneenoughitem.modernfix.warning.line1")
                    .withStyle(ChatFormatting.AQUA);

            MutableComponent line2Start = Component.translatable("oneenoughitem.modernfix.warning.line2")
                    .withStyle(ChatFormatting.AQUA);

            if (FMLEnvironment.dist == Dist.CLIENT) {
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null) {
                        MutableComponent clickableLink = Component.translatable("oneenoughitem.modernfix.warning.link")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                                .withStyle(style -> style
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                                ModernFixDetector.getConfigPath().toString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.translatable("oneenoughitem.modernfix.warning.hover",
                                                                ModernFixDetector.getConfigPath().toString())
                                                        .withStyle(ChatFormatting.GRAY))));

                        Minecraft.getInstance().player.sendSystemMessage(clickableLink);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            rebuildReplacementCache("data-reload");
            Oneenoughitem.LOGGER.info("Server replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    event.getLoadedCount(), event.getInvalidCount());
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
