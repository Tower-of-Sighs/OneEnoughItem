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
    private static volatile Map<String, String> ReloadOverrideItemMap = null;


    public static String matchItem(String id) {
        return Objects.requireNonNullElse(ReloadOverrideItemMap, ItemMapCache).getOrDefault(id, null);
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

    // 给 JSON 拦截层做快速判断
    public static boolean hasAnyMappings() {
        return !ItemMapCache.isEmpty() || !TagMapCache.isEmpty();
    }

    public static boolean isSourceItemId(String id) {
        if (id == null) return false;
        return Objects.requireNonNullElse(ReloadOverrideItemMap, ItemMapCache).containsKey(id);
    }


    public static boolean isSourceTagId(String id) {
        return id != null && TagMapCache.containsKey(id);
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

    public static Set<String> getAllReplacedItems() {
        return new HashSet<>(ItemMapCache.keySet());
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

    // 开始/结束 “重载期覆盖映射”
    public static void beginReloadOverride(Map<String, String> currentItemMap) {
        if (currentItemMap == null || currentItemMap.isEmpty()) {
            ReloadOverrideItemMap = null;
            return;
        }
        ReloadOverrideItemMap = new HashMap<>(currentItemMap);
        Oneenoughitem.LOGGER.info("Enabled reload-override mapping for this resource reload: {} items",
                ReloadOverrideItemMap.size());
    }

    public static void endReloadOverride() {
        if (ReloadOverrideItemMap != null) {
            Oneenoughitem.LOGGER.info("Disabled reload-override mapping: {} entries", ReloadOverrideItemMap.size());
        }
        ReloadOverrideItemMap = null;
    }

    public static boolean hasReloadOverride() {
        return ReloadOverrideItemMap != null;
    }
}