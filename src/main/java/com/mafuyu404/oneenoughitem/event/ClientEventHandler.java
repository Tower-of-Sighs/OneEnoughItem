package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mafuyu404.oelib.neoforge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (ModKeyMappings.OPEN_EDITOR.consumeClick()) {
            if (client.screen == null && hasCtrlDown(client)) {
                client.setScreen(new ReplacementEditorScreen());
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
        if (manager != null) {
            ReplacementCache.clearCache();

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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
