package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oelib.forge.data.DataManager;
import com.mafuyu404.oelib.forge.event.DataReloadEvent;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.ModKeyMappings;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.access.CreativeModeTabIconRefresher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "oneenoughitem", value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_EDITOR);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (ModKeyMappings.OPEN_EDITOR.consumeClick()) {
                if (mc.screen == null && hasCtrlDown()) {
                    mc.setScreen(new ReplacementEditorScreen());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDataReload(DataReloadEvent event) {
        if (event.isDataType(Replacements.class)) {
            Minecraft.getInstance().execute(() -> {
                rebuildReplacementCache();
                GlobalReplacementCache.rebuild();
                refreshAllCreativeModeTabIcons();
                Oneenoughitem.LOGGER.info("Replacement cache rebuilt due to data reload: {} entries loaded, {} invalid",
                        event.getLoadedCount(), event.getInvalidCount());

                Oneenoughitem.LOGGER.info("Recipe JSON rewrite mode (client): {}", String.valueOf(Config.DATA_REWRITE_MODE.getValue()));

                ReplacementCache.endReloadOverride();
            });
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

    private static boolean hasCtrlDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
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
