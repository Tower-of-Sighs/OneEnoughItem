package com.mafuyu404.oneenoughitem.client.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ModernFixDetector {
    private static final String MODERNFIX_MODID = "modernfix";
    private static final String CONFIG_FILE_NAME = "modernfix-mixins.properties";
    private static final String TARGET_CONFIG_KEY = "mixin.perf.faster_ingredients";

    private static boolean hasShownWarning = false;

    public static boolean isModernFixInstalled() {
        boolean installed = ModList.get().isLoaded(MODERNFIX_MODID);
        Oneenoughitem.LOGGER.debug("ModernFix installed: {}", installed);
        return installed;
    }

    public static boolean isFasterIngredientsEnabled() {
        Path configPath = getConfigPath();
        Oneenoughitem.LOGGER.debug("Checking ModernFix config at: {}", configPath);

        if (!Files.exists(configPath)) {
            Oneenoughitem.LOGGER.warn("ModernFix config file not found: {}", configPath);
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(configPath);
            for (String line : lines) {
                String trimmed = line.trim();
                // 只要包含配置项并设置为 true，不管是否被注释
                if (trimmed.matches("^#?\\s*" + TARGET_CONFIG_KEY + "\\s*=\\s*true.*")) {
                    Oneenoughitem.LOGGER.info("ModernFix faster_ingredients appears to be enabled (even in comment): {}", trimmed);
                    return true;
                }
            }
            Oneenoughitem.LOGGER.info("ModernFix faster_ingredients option not found or not enabled.");
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to read ModernFix config file", e);
        }

        return false;
    }


    public static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(CONFIG_FILE_NAME);
    }

    public static boolean shouldShowWarning() {
        boolean should = !hasShownWarning && isModernFixInstalled() && isFasterIngredientsEnabled();
        Oneenoughitem.LOGGER.debug("Should show warning: {}", should);
        return should;
    }

    public static void markWarningShown() {
        hasShownWarning = true;
        Oneenoughitem.LOGGER.debug("Marked ModernFix warning as shown.");
    }
}
