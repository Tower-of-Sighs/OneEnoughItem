package com.mafuyu404.oneenoughitem.client.gui.util;

import net.neoforged.fml.ModList;

public class CompatUtil {
    public static boolean isKubeJSLoaded() {
        return ModList.get().isLoaded("kubejs");
    }
}
