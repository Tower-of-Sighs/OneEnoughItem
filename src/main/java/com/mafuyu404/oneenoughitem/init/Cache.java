package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;

import java.util.HashMap;
import java.util.Map;

public class Cache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        String result = ItemMapCache.getOrDefault(id, null);
        if (result != null) {
            Oneenoughitem.LOGGER.info("Cache hit: {} -> {}", id, result);
        }
        return result;
    }

    public static void putReplacement(Replacements replacement) {
        for (String target : replacement.matchItems()) {
            ItemMapCache.put(target, replacement.resultItems());
            Oneenoughitem.LOGGER.info("Added to cache: {} -> {}", target, replacement.resultItems());
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