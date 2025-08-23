package com.mafuyu404.oneenoughitem.client.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ModernFixDetector {
    private static final String MODERNFIX_MODID = "modernfix";
    private static final String CONFIG_FILE_NAME = "modernfix-mixins.properties";
    private static final String PLAYER_NBT_KEY = Oneenoughitem.MOD_ID + ".modernfix_warning_shown";

    public static boolean isModernFixInstalled() {
        boolean installed = ModList.get().isLoaded(MODERNFIX_MODID);
        Oneenoughitem.LOGGER.debug("ModernFix installed: {}", installed);
        return installed;
    }

    public static boolean hasPlayerSeenWarning(ServerPlayer player) {
        try {
            return player.getPersistentData().getBoolean(PLAYER_NBT_KEY);
        } catch (Throwable t) {
            Oneenoughitem.LOGGER.debug("Failed to read player's ModernFix warning flag: {}", t.toString());
            return false;
        }
    }

    public static void setPlayerSeenWarning(ServerPlayer player) {
        try {
            player.getPersistentData().putBoolean(PLAYER_NBT_KEY, true);
            Oneenoughitem.LOGGER.debug("Marked ModernFix warning as shown for player {}", player.getGameProfile().getName());
        } catch (Throwable t) {
            Oneenoughitem.LOGGER.warn("Failed to set player's ModernFix warning flag", t);
        }
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
    }
}