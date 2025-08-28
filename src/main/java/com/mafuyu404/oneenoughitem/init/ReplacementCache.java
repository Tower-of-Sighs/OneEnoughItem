package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.*;

public class ReplacementCache {
    private static final Map<String, String> ItemMapCache = new HashMap<>();
    private static final HashMap<String, String> TagMapCache = new HashMap<>();
    private static final HashMap<String, Replacements.Rules> ItemRulesCache = new HashMap<>();
    private static final HashMap<String, Replacements.Rules> TagRulesCache = new HashMap<>();
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

    public static Optional<Replacements.Rules> getItemRules(String itemId) {
        return Optional.ofNullable(ItemRulesCache.get(itemId));
    }

    public static Optional<Replacements.Rules> getTagRules(String tagId) {
        return Optional.ofNullable(TagRulesCache.get(tagId));
    }

    private static Optional<Replacements.Rules> getGlobalDefaultRules() {
        try {
            var cfg = ModConfig.DEFAULT_RULES.getValue();
            if (cfg != null) {
                return Optional.of(cfg.toRules());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public static boolean shouldReplaceInDataDir(String itemId, String directory) {
        return getItemRules(itemId)
                .or(ReplacementCache::getGlobalDefaultRules)
                .flatMap(Replacements.Rules::data)
                .map(dataRules -> dataRules.get(directory))
                .map(mode -> mode == Replacements.ProcessingMode.REPLACE)
                .orElse(false);
    }

    public static boolean shouldReplaceInTagType(String itemId, String tagType) {
        return getItemRules(itemId)
                .or(ReplacementCache::getGlobalDefaultRules)
                .flatMap(Replacements.Rules::tag)
                .map(tagRules -> tagRules.get(tagType))
                .map(mode -> mode == Replacements.ProcessingMode.REPLACE)
                .orElse(false);
    }


    public static void putReplacement(Replacements replacement) {
        List<Item> resolvedItems = Utils.resolveItemList(replacement.matchItems());

        // 处理物品替换
        for (Item item : resolvedItems) {
            String id = Utils.getItemRegistryName(item);
            if (id != null) {
                ItemMapCache.put(id, replacement.resultItems());
                replacement.rules().ifPresent(rules -> TagRulesCache.put(id, rules));
                Oneenoughitem.LOGGER.debug("Added item replacement mapping: {} -> {}", id, replacement.resultItems());
            }
        }

        // 处理标签替换
        for (String matchItem : replacement.matchItems()) {
            if (matchItem.startsWith("#")) {
                String tagId = matchItem.substring(1); // 移除 # 前缀
                TagMapCache.put(tagId, replacement.resultItems());
                replacement.rules().ifPresent(rules -> TagRulesCache.put(tagId, rules));
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
        ItemRulesCache.clear();
        TagRulesCache.clear();
        if (previousItemSize > 0 || previousTagSize > 0) {
            Oneenoughitem.LOGGER.info("Cleared {} cached item mappings and {} cached tag mappings",
                    previousItemSize, previousTagSize);
        }
    }

    /**
     * 从缓存中移除指定的物品替换
     */
    public static boolean removeItemReplacement(String itemId) {
        if (itemId != null && ItemMapCache.containsKey(itemId)) {
            String removed = ItemMapCache.remove(itemId);
            Oneenoughitem.LOGGER.debug("Removed item replacement from runtime cache: {} -> {}", itemId, removed);
            return true;
        }
        return false;
    }

    /**
     * 从缓存中移除指定的标签替换
     */
    public static boolean removeTagReplacement(String tagId) {
        if (tagId != null && TagMapCache.containsKey(tagId)) {
            String removed = TagMapCache.remove(tagId);
            Oneenoughitem.LOGGER.debug("Removed tag replacement from runtime cache: {} -> {}", tagId, removed);
            return true;
        }
        return false;
    }

    /**
     * 批量移除物品和标签替换
     */
    public static void removeReplacements(Collection<String> itemIds, Collection<String> tagIds) {
        boolean changed = false;

        if (itemIds != null) {
            for (String itemId : itemIds) {
                if (removeItemReplacement(itemId)) {
                    changed = true;
                }
            }
        }

        if (tagIds != null) {
            for (String tagId : tagIds) {
                if (removeTagReplacement(tagId)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            Oneenoughitem.LOGGER.info("Removed {} item replacements and {} tag replacements from runtime cache",
                    itemIds != null ? itemIds.size() : 0, tagIds != null ? tagIds.size() : 0);
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