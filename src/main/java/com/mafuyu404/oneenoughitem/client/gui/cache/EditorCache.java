package com.mafuyu404.oneenoughitem.client.gui.cache;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class EditorCache extends BaseCache {
    private static final EditorCache INSTANCE = new EditorCache();


    public record CacheData(Set<String> matchItems, Set<String> matchTags,
                            String resultItem, String resultTag, String fileName) {
    }

    private EditorCache() {
        super("editor_cache.dat", 1);
    }

    @Override
    protected void onInitialized() {
    }

    @Override
    protected void onVersionMismatch(int foundVersion) {
        Oneenoughitem.LOGGER.warn("Editor cache version mismatch, ignoring cache");
    }

    @Override
    protected void onLoadError(IOException e) {
    }

    @Override
    protected void loadData(DataInputStream dis) throws IOException {
        throw new UnsupportedOperationException("Use loadCache() method instead");
    }

    @Override
    protected void saveData(DataOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Use saveCache() method instead");
    }

    public static void saveCache(Set<Item> matchItems, Set<ResourceLocation> matchTags,
                                 Item resultItem, ResourceLocation resultTag, String fileName) {
        INSTANCE.withWriteLock(() -> {
            try {
                INSTANCE.saveToFile(matchItems, matchTags, resultItem, resultTag, fileName);
            } catch (Exception e) {
                Oneenoughitem.LOGGER.error("Failed to save editor cache", e);
            }
        });
    }

    private void saveToFile(Set<Item> matchItems, Set<ResourceLocation> matchTags,
                            Item resultItem, ResourceLocation resultTag, String fileName) {
        try {
            // 确保目录存在
            java.nio.file.Files.createDirectories(cacheFile.getParent());

            // 创建临时文件，确保原子性写入
            java.nio.file.Path tempFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");

            // 如果临时文件已存在，先删除
            if (java.nio.file.Files.exists(tempFile)) {
                java.nio.file.Files.delete(tempFile);
            }

            try (DataOutputStream dos = new DataOutputStream(
                    new java.io.BufferedOutputStream(java.nio.file.Files.newOutputStream(tempFile)))) {

                // 写入版本号
                dos.writeInt(cacheVersion);

                // 写入匹配物品
                dos.writeInt(matchItems.size());
                for (Item item : matchItems) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    dos.writeUTF(id != null ? id.toString() : "");
                }

                // 写入匹配标签
                dos.writeInt(matchTags.size());
                for (ResourceLocation tag : matchTags) {
                    dos.writeUTF(tag.toString());
                }

                // 写入结果物品
                if (resultItem != null) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(resultItem);
                    dos.writeUTF(id != null ? id.toString() : "");
                } else {
                    dos.writeUTF("");
                }

                // 写入结果标签
                dos.writeUTF(resultTag != null ? resultTag.toString() : "");

                // 写入文件名
                dos.writeUTF(fileName != null ? fileName : "");

                dos.flush();
            }

            // 原子性替换文件
            java.nio.file.Files.move(tempFile, cacheFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            Oneenoughitem.LOGGER.debug("Editor cache saved successfully to: {}", cacheFile);

        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to save editor cache to: {}", cacheFile, e);
        }
    }


    public static CacheData loadCache() {
        return INSTANCE.withReadLock(() -> {
            if (!java.nio.file.Files.exists(INSTANCE.cacheFile)) {
                Oneenoughitem.LOGGER.debug("Editor cache file not found: {}", INSTANCE.cacheFile);
                return null;
            }

            try (DataInputStream dis = new DataInputStream(
                    new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(INSTANCE.cacheFile)))) {

                int version = dis.readInt();
                if (version != INSTANCE.cacheVersion) {
                    Oneenoughitem.LOGGER.warn("Editor cache version mismatch, ignoring cache");
                    return null;
                }

                Set<String> matchItems = readStringSet(dis);

                Set<String> matchTags = readStringSet(dis);

                String resultItem = dis.readUTF();
                if (resultItem.isEmpty()) {
                    resultItem = null;
                }

                String resultTag = dis.readUTF();
                if (resultTag.isEmpty()) {
                    resultTag = null;
                }

                String fileName = dis.readUTF();
                if (fileName.isEmpty()) {
                    fileName = null;
                }

                Oneenoughitem.LOGGER.debug("Editor cache loaded successfully from: {}", INSTANCE.cacheFile);
                return new CacheData(matchItems, matchTags, resultItem, resultTag, fileName);

            } catch (IOException e) {
                Oneenoughitem.LOGGER.error("Failed to load editor cache from: {}", INSTANCE.cacheFile, e);
                return null;
            }
        });
    }

    private static Set<String> readStringSet(DataInputStream dis) throws IOException {
        Set<String> result = new HashSet<>();
        int count = dis.readInt();
        for (int i = 0; i < count; i++) {
            String str = dis.readUTF();
            if (!str.isEmpty()) {
                result.add(str);
            }
        }
        return result;
    }

    public static void clearCache() {
        INSTANCE.clearCacheFile();
    }

    public Path getCacheFilePath() {
        return INSTANCE.cacheFile;
    }
}