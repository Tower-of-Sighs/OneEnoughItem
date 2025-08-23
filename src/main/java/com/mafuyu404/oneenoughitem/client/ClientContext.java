package com.mafuyu404.oneenoughitem.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public final class ClientContext {
    private ClientContext() {}

    public static boolean isInCreativeInventory() {
        if (!FMLLoader.getDist().isClient()) {
            return false;
        }
        return ClientOnly.isInCreativeInventory();
    }

    @OnlyIn(Dist.CLIENT)
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