package com.mafuyu404.oneenoughitem.client;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientContext {
    private ClientContext() {
    }

    public static boolean isInCreativeInventory() {
        if (!FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT)) {
            return false;
        }
        return ClientOnly.isInCreativeInventory();
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientOnly {
        private static boolean isInCreativeInventory() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) return false;
            var screen = mc.screen;
            if (screen == null) return false;
            return screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
        }
    }
}