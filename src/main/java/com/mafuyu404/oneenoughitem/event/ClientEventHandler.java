package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.fabric.data.DataManager;
import com.mafuyu404.oelib.fabric.event.DataReloadEvent;
import com.mafuyu404.oelib.fabric.event.impl.Events;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.access.CreativeModeTabIconRefresher;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
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
    }

    public static void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount) {
        if (dataClass == Replacements.class) {
            rebuildReplacementCache();
            GlobalReplacementCache.rebuild();
            refreshAllCreativeModeTabIcons();
            Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                    loadedCount, invalidCount);
            Oneenoughitem.LOGGER.info("Recipe JSON rewrite mode (client): {}", String.valueOf(com.mafuyu404.oneenoughitem.init.ModConfig.DATA_REWRITE_MODE.getValue()));

            ReplacementCache.endReloadOverride();
        }
    }

    private static void refreshAllCreativeModeTabIcons() {
        try {
            for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
                if (tab instanceof CreativeModeTabIconRefresher refresher) {
                    refresher.oei$refreshIconCache();
                }
            }
            Oneenoughitem.LOGGER.info("Refreshed creative mode tab icons after replacement reload");
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to refresh creative mode tab icons", e);
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
