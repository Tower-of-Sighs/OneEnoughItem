package com.mafuyu404.oneenoughitem.client.gui.cache;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GlobalReplacementCache extends BaseCache {
    private static final GlobalReplacementCache INSTANCE = new GlobalReplacementCache();

    private final Map<String, String> replacedItems = new ConcurrentHashMap<>();
    private final Map<String, String> replacedTags = new ConcurrentHashMap<>();
    private final Map<String, String> resultItems = new ConcurrentHashMap<>();
    private final Map<String, String> resultTags = new ConcurrentHashMap<>();

    private GlobalReplacementCache() {
        super("global_replacement_cache.dat", 1);
    }

    @Override
    protected void onInitialized() {
        Oneenoughitem.LOGGER.info("Global replacement cache initialized with {} items and {} tags",
                replacedItems.size(), replacedTags.size());
    }

    @Override
    protected void onVersionMismatch(int foundVersion) {
        Oneenoughitem.LOGGER.warn("Global replacement cache version mismatch, rebuilding cache");
        rebuild();
    }

    @Override
    protected void onLoadError(IOException e) {
        clearAllMaps();
    }

    @Override
    protected void loadData(DataInputStream dis) throws IOException {
        readStringMap(dis, replacedItems);
        readStringMap(dis, replacedTags);

        // 读取结果物品（如果版本支持）
        try {
            readStringMap(dis, resultItems);
            readStringMap(dis, resultTags);
        } catch (IOException e) {
            // 旧版本文件，重建缓存以获取结果物品信息
            Oneenoughitem.LOGGER.info("Old cache format detected, rebuilding to include result tracking");
            rebuild();
        }
    }

    @Override
    protected void saveData(DataOutputStream dos) throws IOException {
        writeStringMap(dos, replacedItems);
        writeStringMap(dos, replacedTags);
        writeStringMap(dos, resultItems);
        writeStringMap(dos, resultTags);
    }

    public static void addReplacement(Collection<String> matchItems, Collection<String> matchTags,
                                      String resultItem, String resultTag, String sourceFile) {
        INSTANCE.withInitializedWriteLock(() -> {
            String result = resultItem != null ? resultItem : resultTag;
            if (result == null) {
                Oneenoughitem.LOGGER.warn("Cannot add replacement with null result from file: {}", sourceFile);
                return;
            }

            // 添加物品和标签替换
            processValidStrings(matchItems, itemId -> INSTANCE.replacedItems.put(itemId, result));
            processValidStrings(matchTags, tagId -> INSTANCE.replacedTags.put(tagId, result));

            // 跟踪结果物品/标签
            addToResultMap(resultItem, sourceFile, INSTANCE.resultItems);
            addToResultMap(resultTag, sourceFile, INSTANCE.resultTags);

            INSTANCE.saveToFileAsync();
        });
    }


    public static void removeReplacement(Collection<String> matchItems, Collection<String> matchTags) {
        INSTANCE.withInitializedWriteLock(() -> {
            boolean changed = false;

            // 移除物品和标签替换
            changed |= removeFromReplacementMap(matchItems, INSTANCE.replacedItems, "item");
            changed |= removeFromReplacementMap(matchTags, INSTANCE.replacedTags, "tag");

            if (changed) {
                INSTANCE.saveToFileAsync();
                Oneenoughitem.LOGGER.debug("Global replacement cache updated after removal");
            }
        });
    }

    public static boolean isItemReplaced(Item item) {
        if (item == null) return false;
        String itemId = Utils.getItemRegistryName(item);
        return isItemReplaced(itemId);
    }

    public static boolean isItemReplaced(String itemId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(itemId) && INSTANCE.replacedItems.containsKey(itemId));
    }

    public static boolean isTagReplaced(ResourceLocation tagId) {
        return tagId != null && isTagReplaced(tagId.toString());
    }

    public static boolean isTagReplaced(String tagId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(tagId) && INSTANCE.replacedTags.containsKey(tagId));
    }

    public static String getItemReplacement(String itemId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(itemId) ? INSTANCE.replacedItems.get(itemId) : null);
    }

    public static String getTagReplacement(String tagId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(tagId) ? INSTANCE.replacedTags.get(tagId) : null);
    }

    public static boolean isItemUsedAsResult(String itemId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(itemId) && INSTANCE.resultItems.containsKey(itemId));
    }

    public static boolean isTagUsedAsResult(String tagId) {
        return INSTANCE.withInitializedReadLock(() ->
                isValidString(tagId) && INSTANCE.resultTags.containsKey(tagId));
    }

    public static void clearAll() {
        INSTANCE.withInitializedWriteLock(() -> {
            INSTANCE.clearAllMaps();
            INSTANCE.saveToFileAsync();
            Oneenoughitem.LOGGER.info("Global replacement cache cleared");
        });
    }

    public static void rebuild() {
        INSTANCE.withInitializedWriteLock(() -> {
            Oneenoughitem.LOGGER.info("Starting global replacement cache rebuild...");

            int oldItemCount = INSTANCE.replacedItems.size();
            int oldTagCount = INSTANCE.replacedTags.size();

            INSTANCE.clearAllMaps();
            rebuildFromJsonFiles();
            INSTANCE.saveToFileAsync();

            Oneenoughitem.LOGGER.info("Global replacement cache rebuilt: {} -> {} items, {} -> {} tags",
                    oldItemCount, INSTANCE.replacedItems.size(), oldTagCount, INSTANCE.replacedTags.size());
        });
    }

    public static String getStats() {
        return INSTANCE.withInitializedReadLock(() ->
                String.format("Items: %d, Tags: %d, Result Items: %d, Result Tags: %d",
                        INSTANCE.replacedItems.size(), INSTANCE.replacedTags.size(),
                        INSTANCE.resultItems.size(), INSTANCE.resultTags.size()));
    }

    // ==================== Tool ====================

    /**
     * 读取字符串映射
     */
    private static void readStringMap(DataInputStream dis, Map<String, String> map) throws IOException {
        int count = dis.readInt();
        for (int i = 0; i < count; i++) {
            String key = dis.readUTF();
            String value = dis.readUTF();
            if (isValidString(key) && isValidString(value)) {
                map.put(key, value);
            }
        }
    }

    /**
     * 写入字符串映射
     */
    private static void writeStringMap(DataOutputStream dos, Map<String, String> map) throws IOException {
        dos.writeInt(map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            dos.writeUTF(entry.getKey());
            dos.writeUTF(entry.getValue());
        }
    }

    /**
     * 验证字符串是否有效（非空且不为null）
     */
    private static boolean isValidString(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * 判断是否为标签（以#开头）
     */
    private static boolean isTag(String str) {
        return str != null && str.startsWith("#");
    }

    /**
     * 移除标签前缀
     */
    private static String removeTagPrefix(String tag) {
        return isTag(tag) ? tag.substring(1) : tag;
    }

    /**
     * 处理有效字符串集合
     */
    private static void processValidStrings(Collection<String> strings, Consumer<String> processor) {
        if (strings != null) {
            strings.stream()
                    .filter(GlobalReplacementCache::isValidString)
                    .forEach(processor);
        }
    }

    /**
     * 添加到结果映射
     */
    private static void addToResultMap(String item, String source, Map<String, String> resultMap) {
        if (isValidString(item)) {
            resultMap.put(item, source);
        }
    }

    /**
     * 从替换映射中移除项目
     */
    private static boolean removeFromReplacementMap(Collection<String> items,
                                                    Map<String, String> replacementMap,
                                                    String itemType) {
        boolean changed = false;
        if (items != null) {
            for (String item : items) {
                if (isValidString(item)) {
                    String removedResult = replacementMap.remove(item);
                    if (removedResult != null) {
                        changed = true;
                        Oneenoughitem.LOGGER.debug("Removed {} replacement: {}", itemType, item);
                        removeFromResultMaps(removedResult);
                    }
                }
            }
        }
        return changed;
    }

    /**
     * 从结果映射中移除项目
     */
    private static void removeFromResultMaps(String result) {
        if (isTag(result)) {
            INSTANCE.resultTags.remove(removeTagPrefix(result));
        } else {
            INSTANCE.resultItems.remove(result);
        }
    }

    /**
     * 清空所有映射
     */
    private void clearAllMaps() {
        replacedItems.clear();
        replacedTags.clear();
        resultItems.clear();
        resultTags.clear();
    }

    /**
     * 带初始化的读锁操作
     */
    private <T> T withInitializedReadLock(Supplier<T> operation) {
        initialize();
        return withReadLock(operation);
    }

    /**
     * 带初始化的写锁操作
     */
    private void withInitializedWriteLock(Runnable operation) {
        initialize();
        withWriteLock(operation);
    }

    // ==================== Rebuild ====================

    private static void rebuildFromJsonFiles() {
        try {
            var fileInfos = PathUtils.scanAllReplacementFiles();
            fileInfos.forEach(GlobalReplacementCache::processReplacementFile);

            Oneenoughitem.LOGGER.info("Rebuilt global replacement cache from {} files: {} items, {} tags, {} result items, {} result tags",
                    fileInfos.size(), INSTANCE.replacedItems.size(), INSTANCE.replacedTags.size(),
                    INSTANCE.resultItems.size(), INSTANCE.resultTags.size());

        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to rebuild global replacement cache from JSON files", e);
        }
    }

    private static void processReplacementFile(PathUtils.FileInfo fileInfo) {
        try {
            String content = Files.readString(fileInfo.filePath());
            if (content.trim().isEmpty()) {
                Oneenoughitem.LOGGER.debug("Skipping empty file: {}", fileInfo.filePath());
                return;
            }

            JsonElement parsed = new Gson().fromJson(content, JsonElement.class);
            if (parsed != null && parsed.isJsonArray()) {
                JsonArray jsonArray = parsed.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    processReplacementElement(element, fileInfo);
                }
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to read replacement file: {}", fileInfo.filePath(), e);
        }
    }

    private static void processReplacementElement(JsonElement element, PathUtils.FileInfo fileInfo) {
        try {
            var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);

            if (result.result().isPresent()) {
                var replacement = result.result().get();
                String resultString = replacement.resultItems();

                // 处理匹配项
                for (String matchItem : replacement.matchItems()) {
                    if (isTag(matchItem)) {
                        INSTANCE.replacedTags.put(removeTagPrefix(matchItem), resultString);
                    } else {
                        INSTANCE.replacedItems.put(matchItem, resultString);
                    }
                }

                // 跟踪结果物品/标签
                if (isTag(resultString)) {
                    INSTANCE.resultTags.put(removeTagPrefix(resultString), fileInfo.filePath().toString());
                } else {
                    INSTANCE.resultItems.put(resultString, fileInfo.filePath().toString());
                }
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to parse replacement rule in file: {}", fileInfo.filePath(), e);
        }
    }
}