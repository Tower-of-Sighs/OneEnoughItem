package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.*;

public class ReplacementCache {
    private static final Map<String, String> ItemMapCache = new HashMap<>();
    private static final HashMap<String, String> TagMapCache = new HashMap<>();
    private static final Map<String, Set<String>> ResultToSources = new HashMap<>();

    private static void addMapping(String sourceItemId, String resultItemId) {
        String prev = ItemMapCache.put(sourceItemId, resultItemId);
        if (prev != null && !prev.equals(resultItemId)) {
            Set<String> prevSources = ResultToSources.get(prev);
            if (prevSources != null) {
                prevSources.remove(sourceItemId);
                if (prevSources.isEmpty()) {
                    ResultToSources.remove(prev);
                }
            }
        }
        ResultToSources.computeIfAbsent(resultItemId, k -> new HashSet<>()).add(sourceItemId);
    }

    public static void putReplacement(Replacements replacement, HolderLookup.RegistryLookup<Item> registryLookup) {
        List<Item> resolvedItems = Collections.emptyList();
        int directCount = 0;
        int tagCount = (int) replacement.matchItems().stream().filter(s -> s != null && s.startsWith("#")).count();

        if (registryLookup != null) {
            resolvedItems = Utils.resolveItemList(replacement.matchItems(), registryLookup);
            for (Item item : resolvedItems) {
                String id = Utils.getItemRegistryName(item);
                if (id != null) {
                    addMapping(id, replacement.resultItems());
                    Oneenoughitem.LOGGER.debug("Added item replacement mapping: {} -> {}", id, replacement.resultItems());
                }
            }

            for (String matchItem : replacement.matchItems()) {
                if (matchItem != null && matchItem.startsWith("#")) {
                    String tagId = matchItem.substring(1);
                    TagMapCache.put(tagId, replacement.resultItems());
                    Oneenoughitem.LOGGER.debug("Added tag replacement mapping: {} -> {}", tagId, replacement.resultItems());
                }
            }
        } else {
            for (String matchItem : replacement.matchItems()) {
                if (matchItem != null && !matchItem.isEmpty() && !matchItem.startsWith("#")) {
                    addMapping(matchItem, replacement.resultItems());
                    directCount++;
                    Oneenoughitem.LOGGER.debug("Added item replacement mapping (no-registry): {} -> {}", matchItem, replacement.resultItems());
                }
            }
            if (tagCount > 0) {
                Oneenoughitem.LOGGER.debug("Deferred {} tag mappings until registry becomes available", tagCount);
            }
        }

        int itemCount = (registryLookup != null) ? resolvedItems.size() : directCount;
        if (itemCount == 0 && tagCount == 0) {
            Oneenoughitem.LOGGER.warn("No valid items or tags resolved from matchItems: {}", replacement.matchItems());
        } else {
            Oneenoughitem.LOGGER.info("Added replacement rule for {} items and {} tags: {} -> {}",
                    itemCount, tagCount, replacement.matchItems(), replacement.resultItems());
        }
    }

    public static void putReplacementDirect(String sourceItemId, String targetItemId) {
        if (sourceItemId != null && targetItemId != null) {
            addMapping(sourceItemId, targetItemId);
        }
    }

    public static void putReplacementsBatch(Map<String, String> mappings) {
        for (Map.Entry<String, String> e : mappings.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                addMapping(e.getKey(), e.getValue());
            }
        }
    }

    public static void clearCache() {
        int previousItemSize = ItemMapCache.size();
        int previousTagSize = TagMapCache.size();
        ItemMapCache.clear();
        TagMapCache.clear();
        ResultToSources.clear();
        if (previousItemSize > 0 || previousTagSize > 0) {
            Oneenoughitem.LOGGER.info("Cleared {} cached item mappings and {} cached tag mappings",
                    previousItemSize, previousTagSize);
        }
    }

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

    /**
     * 获取所有已替换的物品ID
     */
    public static Set<String> getAllReplacedItems() {
        return new HashSet<>(ItemMapCache.keySet());
    }

    /**
     * 获取所有以某结果为目标的源物品ID集合（反向索引查找，零分配）
     */
    public static Set<String> trackSourceOf(String id) {
        Set<String> set = ResultToSources.get(id);
        return set != null ? set : Collections.emptySet();
    }
}