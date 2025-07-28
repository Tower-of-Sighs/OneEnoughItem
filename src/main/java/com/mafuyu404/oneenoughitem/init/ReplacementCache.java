package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;

public class ReplacementCache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static void putReplacement(Replacements replacement) {
        List<Item> resolvedItems = Utils.resolveItemList(replacement.matchItems());

        for (Item item : resolvedItems) {
            String id = Utils.getItemRegistryName(item);
            if (id != null) {
                ItemMapCache.put(id, replacement.resultItems());
                Oneenoughitem.LOGGER.debug("Added replacement mapping: {} -> {}", id, replacement.resultItems());
            }
        }

        if (resolvedItems.isEmpty()) {
            Oneenoughitem.LOGGER.warn("No valid items resolved from matchItems: {}", replacement.matchItems());
        } else {
            Oneenoughitem.LOGGER.info("Added replacement rule for {} items: {} -> {}",
                    resolvedItems.size(), replacement.matchItems(), replacement.resultItems());
        }
    }

    public static void clearCache() {
        int previousSize = ItemMapCache.size();
        ItemMapCache.clear();
        if (previousSize > 0) {
            Oneenoughitem.LOGGER.info("Cleared {} cached item mappings", previousSize);
        }
    }

    public static int getCacheSize() {
        return ItemMapCache.size();
    }
}