package com.mafuyu404.oneenoughitem.client.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Config;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ModernFixDetector {
    private static final String MODERNFIX_MODID = "modernfix";
    private static final String CONFIG_FILE_NAME = "modernfix-mixins.properties";

    public static boolean isModernFixInstalled() {
        boolean installed = ModList.get().isLoaded(MODERNFIX_MODID);
        Oneenoughitem.LOGGER.debug("ModernFix installed: {}", installed);
        return installed;
    }

    public static boolean shouldShowWarning() {
        boolean should = isModernFixInstalled() && !Config.IS_MODERNFIX_WARNING_SHOWN;
        Oneenoughitem.LOGGER.debug("Should show ModernFix warning: {}", should);
        return should;
    }

    public static void markWarningShown() {
        Config.IS_MODERNFIX_WARNING_SHOWN = true;

        if (Config.CONFIG_LOADED) {
            try {
                Config.MODERNFIX_WARNING_SHOWN.set(true);
            } catch (IllegalStateException e) {
                Oneenoughitem.LOGGER.debug("Config not ready, will be saved on next config load");
            }
        } else {
            Oneenoughitem.LOGGER.debug("Config not loaded yet; deferring saving flag until after load.");
        }

        Oneenoughitem.LOGGER.debug("Marked ModernFix warning as shown.");
    }


    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }
}
