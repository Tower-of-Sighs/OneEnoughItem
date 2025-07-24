package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;

import java.util.HashMap;

public class Cache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static void putReplacement(Replacements replacement) {
        for (String target : replacement.matchItems()) {
            if (Utils.getItemById(target) != null) {
                ItemMapCache.put(target, replacement.resultItems());
            }
        }
    }

    public static void clearCache() {
        int previousSize = ItemMapCache.size();
        ItemMapCache.clear();
        if (previousSize > 0) {
            Oneenoughitem.LOGGER.info("Cleared {} cached item mappings", previousSize);
        }
    }
}