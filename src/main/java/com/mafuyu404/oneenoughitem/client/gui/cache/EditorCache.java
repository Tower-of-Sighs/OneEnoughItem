package com.mafuyu404.oneenoughitem.client.gui.cache;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class EditorCache {
    private static final Path CACHE_FILE = Paths.get("config", "oneenoughitem_editor_cache.dat");

    public record CacheData(Set<String> matchItems, Set<String> matchTags, String resultItem, String resultTag,
                            String fileName) {
    }

    public static void saveCache(Set<Item> matchItems, Set<ResourceLocation> matchTags,
                                Item resultItem, ResourceLocation resultTag, String fileName) {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(CACHE_FILE)))) {

                dos.writeInt(matchItems.size());
                for (Item item : matchItems) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    dos.writeUTF(id != null ? id.toString() : "");
                }

                dos.writeInt(matchTags.size());
                for (ResourceLocation tag : matchTags) {
                    dos.writeUTF(tag.toString());
                }

                if (resultItem != null) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(resultItem);
                    dos.writeUTF(id != null ? id.toString() : "");
                } else {
                    dos.writeUTF("");
                }

                dos.writeUTF(resultTag != null ? resultTag.toString() : "");

                dos.writeUTF(fileName != null ? fileName : "");
                
                dos.flush();
            }
            
            Oneenoughitem.LOGGER.info("Editor cache saved successfully");
            
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to save editor cache", e);
        }
    }

    public static CacheData loadCache() {
        if (!Files.exists(CACHE_FILE)) {
            return null;
        }
        
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(CACHE_FILE)))) {

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
            
            Oneenoughitem.LOGGER.info("Editor cache loaded successfully");
            return new CacheData(matchItems, matchTags, resultItem, resultTag, fileName);
            
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to load editor cache", e);
            return null;
        }
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
        try {
            if (Files.exists(CACHE_FILE)) {
                Files.delete(CACHE_FILE);
                Oneenoughitem.LOGGER.info("Editor cache cleared");
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to clear editor cache", e);
        }
    }
}