package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.api.EditableItem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.world.item.Item;

import java.util.HashMap;

public class ReplacementCache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static void putReplacement(Replacements replacement) {
        for (String target : replacement.matchItems()) {
            Item item = Utils.getItemById(target);
            if (item != null) {
                ItemMapCache.put(target, replacement.resultItems());
//                ((EditableItem) item).setFoodProperties(null);
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