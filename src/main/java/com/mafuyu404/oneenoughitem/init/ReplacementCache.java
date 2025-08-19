package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.*;

public class ReplacementCache {
    private static final HashMap<String, String> ItemMapCache = new HashMap<>();
    private static final HashMap<String, String> TagMapCache = new HashMap<>();

    public static HashMap<String, String> getItemMapCache() {
        return ItemMapCache;
    }

    public static String matchItem(String id) {
        String direct = ItemMapCache.get(id);
        if (direct != null) return direct;

        Item item = Utils.getItemById(id);
        if (item == null) return null;

        List<ResourceLocation> tags = Utils.getItemTags(item);
        if (tags.isEmpty()) return null;

        for (ResourceLocation tag : tags) {
            String mapped = matchTag(tag);
            if (mapped != null) return mapped;
        }
        return null;
    }

    public static String matchTag(String tagId) {
        return TagMapCache.getOrDefault(tagId, null);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }

    public static void putReplacement(Replacement replacement) {
        for (String item : replacement.getMatchItems()) {
            if (item.startsWith("#")) {
                TagMapCache.put(item.replace("#", ""), replacement.getResultItems());
            } else if (!item.equals(replacement.getResultItems())) {
                ItemMapCache.put(item, replacement.getResultItems());
            }
        }
    }

    public static void clearCache() {
        int previousItemSize = ItemMapCache.size();
        int previousTagSize = TagMapCache.size();
        ItemMapCache.clear();
        TagMapCache.clear();
        if (previousItemSize > 0 || previousTagSize > 0) {
            Oneenoughitem.LOGGER.info("Cleared {} cached item mappings and {} cached tag mappings",
                    previousItemSize, previousTagSize);
        }
    }

    public static Collection<String> trackSourceIdOf(String id) {
        Collection<String> result = new HashSet<>();
        ItemMapCache.forEach((matchItem, resultItem) -> {
            if (resultItem.equals(id)) result.add(matchItem);
        });
        return result;
    }
    public static Collection<Item> trackSourceOf(String id) {
        Collection<Item> result = new HashSet<>();
        ItemMapCache.forEach((matchItem, resultItem) -> {
            if (resultItem.equals(id)) result.add(Utils.getItemById(matchItem));
        });
        return result;
    }
}