package com.mafuyu404.oneenoughitem.client.gui.util;


import net.fabricmc.loader.api.FabricLoader;

public class CompatUtil {
    public static boolean isKubeJSLoaded() {
        return FabricLoader.getInstance().isModLoaded("kubejs");
    }
}