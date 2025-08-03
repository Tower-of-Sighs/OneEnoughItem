package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplacementCache {
    private static final Map<String, String> ItemMapCache = new HashMap<>();
    private static final HashMap<String, String> TagMapCache = new HashMap<>();

    public static String matchItem(String id) {
        return ItemMapCache.getOrDefault(id, null);
    }

    public static String matchTag(String tagId) {
        return TagMapCache.getOrDefault(tagId, null);
    }

    public static String matchTag(ResourceLocation tagId) {
        return tagId != null ? matchTag(tagId.toString()) : null;
    }

    public static boolean isTagReplaced(String tagId) {
        return tagId != null && TagMapCache.containsKey(tagId);
    }

    public static boolean isTagReplaced(ResourceLocation tagId) {
        return tagId != null && isTagReplaced(tagId.toString());
    }

    public static void putReplacement(Replacements replacement) {
        List<Item> resolvedItems = Utils.resolveItemList(replacement.matchItems());

        // 处理物品替换
        for (Item item : resolvedItems) {
            String id = Utils.getItemRegistryName(item);
            if (id != null) {
                ItemMapCache.put(id, replacement.resultItems());
                Oneenoughitem.LOGGER.debug("Added item replacement mapping: {} -> {}", id, replacement.resultItems());
            }
        }

        // 处理标签替换
        for (String matchItem : replacement.matchItems()) {
            if (matchItem.startsWith("#")) {
                String tagId = matchItem.substring(1); // 移除 # 前缀
                TagMapCache.put(tagId, replacement.resultItems());
                Oneenoughitem.LOGGER.debug("Added tag replacement mapping: {} -> {}", tagId, replacement.resultItems());
            }
        }

        if (resolvedItems.isEmpty() && replacement.matchItems().stream().noneMatch(s -> s.startsWith("#"))) {
            Oneenoughitem.LOGGER.warn("No valid items or tags resolved from matchItems: {}", replacement.matchItems());
        } else {
            int tagCount = (int) replacement.matchItems().stream().filter(s -> s.startsWith("#")).count();
            Oneenoughitem.LOGGER.info("Added replacement rule for {} items and {} tags: {} -> {}",
                    resolvedItems.size(), tagCount, replacement.matchItems(), replacement.resultItems());
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

    public static int getCacheSize() {
        return ItemMapCache.size() + TagMapCache.size();
    }

    public static int getItemCacheSize() {
        return ItemMapCache.size();
    }

    public static int getTagCacheSize() {
        return TagMapCache.size();
    }
}