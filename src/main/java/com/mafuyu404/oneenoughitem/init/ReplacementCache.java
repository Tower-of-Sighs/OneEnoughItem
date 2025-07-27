package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplacementCache {
    private static final Map<String, String> ItemMapCache = new HashMap<>();

    public static void putReplacement(Replacements replacement, HolderLookup.RegistryLookup<Item> registryLookup) {
        List<Item> resolvedItems = Utils.resolveItemList(replacement.matchItems(), registryLookup);
        for (Item item : resolvedItems) {
            String id = Utils.getItemRegistryName(item);
            if (id != null) {
                ItemMapCache.put(id, replacement.resultItems());
            }
        }
    }

    public static void putReplacementDirect(String sourceItemId, String targetItemId) {
        if (sourceItemId != null && targetItemId != null) {
            ItemMapCache.put(sourceItemId, targetItemId);
        }
    }

    public static void putReplacementsBatch(Map<String, String> mappings) {
        ItemMapCache.putAll(mappings);
    }

    public static void clearCache() {
        ItemMapCache.clear();
        Oneenoughitem.LOGGER.info("Cache cleared");
    }

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static Map<String, String> getCacheContents() {
        return new HashMap<>(ItemMapCache);
    }
}