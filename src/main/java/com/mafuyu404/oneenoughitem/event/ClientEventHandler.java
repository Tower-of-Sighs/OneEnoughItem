package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mafuyu404.oelib.neoforge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (ModKeyMappings.OPEN_EDITOR.consumeClick()) {
            if (client.screen == null && hasCtrlDown(client)) {
                client.setScreen(new  ReplacementEditorScreen());
            }
        }
    }

    private static boolean hasCtrlDown(Minecraft client) {
        long window = client.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            Minecraft.getInstance().execute(() -> {
                rebuildReplacementCache();
                GlobalReplacementCache.rebuild();
                Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                        event.getLoadedCount(), event.getInvalidCount());
            });
        }
    }

    private static void rebuildReplacementCache() {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager == null) {
            Oneenoughitem.LOGGER.warn("No replacement data manager found in OELib");
            return;
        }

        ReplacementCache.clearCache();
        var replacements = manager.getDataList();

        MinecraftServer server = DataManager.getCurrentServer();
        if (server == null) {
            Oneenoughitem.LOGGER.warn("No server instance available, cannot rebuild replacement cache with registry lookup");
            for (Replacements replacement : replacements) {
                ReplacementCache.putReplacement(replacement, null);
            }
            Oneenoughitem.LOGGER.debug(
                    "Rebuilt replacement cache (client provisional without registry) with {} rules",
                    replacements.size()
            );
            return;
        }

        HolderLookup.RegistryLookup<Item> registryLookup = server.registryAccess().lookupOrThrow(Registries.ITEM);
        for (Replacements replacement : replacements) {
            ReplacementCache.putReplacement(replacement, registryLookup);
        }
        Oneenoughitem.LOGGER.debug(
                "Rebuilt replacement cache with {} rules from OELib data manager",
                replacements.size()
        );
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!ModernFixDetector.isModernFixInstalled() || ModernFixDetector.hasPlayerSeenWarning(player)) {
            return;
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null) {
                    // line1
                    MutableComponent line1 = Component.translatable("oneenoughitem.modernfix.warning.line1")
                            .withStyle(ChatFormatting.AQUA);
                    Minecraft.getInstance().player.sendSystemMessage(line1);

                    // line2
                    MutableComponent line2 = Component.translatable("oneenoughitem.modernfix.warning.line2")
                            .withStyle(ChatFormatting.AQUA);
                    Minecraft.getInstance().player.sendSystemMessage(line2);

                    // clickable link
                    MutableComponent clickableLink = Component.translatable("oneenoughitem.modernfix.warning.link")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.OPEN_FILE,
                                            ModernFixDetector.getConfigPath().toString()
                                    ))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable(
                                                    "oneenoughitem.modernfix.warning.hover",
                                                    ModernFixDetector.getConfigPath().toString()
                                            ).withStyle(ChatFormatting.GRAY)
                                    )));
                    Minecraft.getInstance().player.sendSystemMessage(clickableLink);
                }
            });
        }
    }

}
