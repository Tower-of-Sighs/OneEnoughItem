package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.fabric.data.DataManager;
import com.mafuyu404.oelib.fabric.event.DataReloadEvent;
import com.mafuyu404.oelib.fabric.event.impl.Events;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.util.ModernFixDetector;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public class ClientEventHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ModKeyMappings.OPEN_EDITOR.consumeClick()) {
                if (client.screen == null && hasCtrlDown(client)) {
                    client.setScreen(new ReplacementEditorScreen());
                }
            }
        });
        Events.on(DataReloadEvent.EVENT)
                .normal()
                .register(ClientEventHandler::onDataReload);

        Events.on(ClientPlayConnectionEvents.JOIN)
                .highest()
                .register(ClientEventHandler::onPlayerJoin);
    }

    public static void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount) {
        if (dataClass == Replacements.class) {
            rebuildReplacementCache();
            GlobalReplacementCache.rebuild();
            Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    loadedCount, invalidCount);
        }
    }

    public static void onPlayerJoin(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        if (ModernFixDetector.shouldShowWarning()) {
            ModernFixDetector.markWarningShown();

            LocalPlayer player = client.player;
            if (player == null) return;

            MutableComponent line1 = Component.translatable("oneenoughitem.modernfix.warning.line1")
                    .withStyle(ChatFormatting.AQUA);

            MutableComponent line2Start = Component.translatable("oneenoughitem.modernfix.warning.line2")
                    .withStyle(ChatFormatting.AQUA);

            MutableComponent clickableLink = Component.translatable("oneenoughitem.modernfix.warning.link")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, ModernFixDetector.getConfigPath().toString()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("oneenoughitem.modernfix.warning.hover", ModernFixDetector.getConfigPath().toString())
                                            .withStyle(ChatFormatting.GRAY))));

            MutableComponent line2End = Component.translatable("oneenoughitem.modernfix.warning.suffix")
                    .withStyle(ChatFormatting.AQUA);

            MutableComponent line2 = line2Start.append(clickableLink).append(line2End);

            player.sendSystemMessage(line1);
            player.sendSystemMessage(line2);
        }
    }

    private static boolean hasCtrlDown(Minecraft client) {
        long window = client.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static void rebuildReplacementCache() {
        DataManager<Replacements> manager = DataManager.get(Replacements.class);
        if (manager != null) {
            ReplacementCache.clearCache();

            var replacements = manager.getDataList();
            for (Replacements replacement : replacements) {
                ReplacementCache.putReplacement(replacement);
            }

            Oneenoughitem.LOGGER.debug("Rebuilt replacement cache with {} rules from OELib data manager",
                    replacements.size());
        } else {
            Oneenoughitem.LOGGER.warn("No replacement data manager found in OELib");
        }
    }
}
