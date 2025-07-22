package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.data.Replacements;

import java.util.HashMap;

public class Cache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static void putReplacement(Replacements replacement) {
        for (String matchItem : replacement.matchItems()) {
            ItemMapCache.put(matchItem, replacement.resultItem());
        }
    }
}
