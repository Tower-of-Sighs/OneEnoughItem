package com.mafuyu404.oneenoughitem.client.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Config;
import net.minecraftforge.fml.ModList;

public class ModernFixDetector {
    private static final String MODERNFIX_MODID = "modernfix";

    public static boolean isModernFixInstalled() {
        boolean installed = ModList.get().isLoaded(MODERNFIX_MODID);
        Oneenoughitem.LOGGER.debug("ModernFix installed: {}", installed);
        return installed;
    }

    public static boolean shouldShowWarning() {
        boolean should = isModernFixInstalled() && !Config.MODERNFIX_WARNING_SHOWN.get();
        Oneenoughitem.LOGGER.debug("Should show ModernFix warning: {}", should);
        return should;
    }

    public static void markWarningShown() {
        Config.MODERNFIX_WARNING_SHOWN.set(true);
        Config.SPEC.save();
        Oneenoughitem.LOGGER.debug("Marked ModernFix warning as shown and saved config.");
    }
}