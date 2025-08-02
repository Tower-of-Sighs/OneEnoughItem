package com.mafuyu404.oneenoughitem.client.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ModernFixDetector {
    private static final String MODERNFIX_MODID = "modernfix";
    private static final String CONFIG_FILE_NAME = "modernfix-mixins.properties";
    private static final String TARGET_CONFIG_KEY = "mixin.perf.faster_ingredients";
    
    private static boolean hasShownWarning = false;

    public static boolean isModernFixInstalled() {
        return ModList.get().isLoaded(MODERNFIX_MODID);
    }

    public static boolean isFasterIngredientsEnabled() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            return false;
        }
        
        try {
            List<String> lines = Files.readAllLines(configPath);
            for (String line : lines) {
                String trimmed = line.trim();
                // 检查未注释的配置行
                if (!trimmed.startsWith("#") && trimmed.startsWith(TARGET_CONFIG_KEY)) {
                    String[] parts = trimmed.split("=", 2);
                    if (parts.length == 2) {
                        String value = parts[1].trim().split("#")[0].trim(); // 移除行末注释
                        return "true".equalsIgnoreCase(value);
                    }
                }
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to read ModernFix config file", e);
        }
        
        return false;
    }

    public static boolean disableFasterIngredients() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            Oneenoughitem.LOGGER.warn("ModernFix config file not found: {}", configPath);
            return false;
        }
        
        try {
            List<String> lines = Files.readAllLines(configPath);
            List<String> newLines = new ArrayList<>();
            boolean modified = false;
            
            for (String line : lines) {
                String trimmed = line.trim();
                // 找到目标配置行
                if (!trimmed.startsWith("#") && trimmed.startsWith(TARGET_CONFIG_KEY)) {
                    // 将其设置为false并添加注释
                    newLines.add(TARGET_CONFIG_KEY + "=false # Disabled by OneEnoughItem to prevent recipe conflicts");
                    modified = true;
                } else {
                    newLines.add(line);
                }
            }
            
            if (modified) {
                Files.write(configPath, newLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                Oneenoughitem.LOGGER.info("Successfully disabled ModernFix faster_ingredients option");
                return true;
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to modify ModernFix config file", e);
        }
        
        return false;
    }

    private static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE_NAME);
    }

    public static boolean shouldShowWarning() {
        return !hasShownWarning && isModernFixInstalled() && isFasterIngredientsEnabled();
    }

    public static void markWarningShown() {
        hasShownWarning = true;
    }
}