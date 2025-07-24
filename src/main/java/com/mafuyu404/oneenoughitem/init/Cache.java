package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static void putReplacement(Replacements replacement) {
        for (String target : replacement.matchItems()) {
            ItemMapCache.put(target, replacement.resultItems());
        }
    }

    public static void clearCache() {
        ItemMapCache.clear();
        Oneenoughitem.LOGGER.info("Cache cleared");
    }

    public static Map<String, String> getCacheContents() {
        return new HashMap<>(ItemMapCache);
    }
}