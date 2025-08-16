package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = null;

        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            if (entry.getValue().equals(item)) {
                registryName = entry.getKey().location();
            }
        }

        if (registryName == null) {
            Oneenoughitem.LOGGER.warn("getItemRegistryName: registryName is null for item: {}", item.getClass().getName());
            return null;
        }

        return registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            Oneenoughitem.LOGGER.debug("getItemById: registryName is null or empty");
            return null;
        }

        try {
            ResourceLocation resourceLocation = new ResourceLocation(registryName);

            if (!ForgeRegistries.ITEMS.containsKey(resourceLocation)) {
                Oneenoughitem.LOGGER.debug("getItemById: Item '{}' not found in registry", registryName);
                return null;
            }

            Item item = null;

            for (var entry : ForgeRegistries.ITEMS.getEntries()) {
                if (entry.getKey().location().equals(resourceLocation)) {
                    item = entry.getValue();
                }
            }

            return item;
        } catch (Exception e) {
            Oneenoughitem.LOGGER.debug("getItemById: Exception while getting item for registry name: {}", registryName, e);
            return null;
        }
    }

    public static List<ResourceLocation> getItemTags(Item item) {
        if (isOldMC()) return List.of();

        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();

        if (tagManager == null) {
            return Collections.emptyList();
        }

        return tagManager.getReverseTag(item)
                .map(reverseTag ->
                        reverseTag.getTagKeys()
                                .map(TagKey::location)
                                .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static void loadAllReplacement() {
        ReplacementCache.clearCache();
        ReplacementLoader.loadAll().forEach(ReplacementCache::putReplacement);
    }

    public static boolean isItemIdEmpty(String id) {
        return id == null || id.equals("minecraft:air");
    }

    public static boolean isOldMC() {
        String ver = FMLLoader.versionInfo().mcVersion();
        return ver.startsWith("1.18") || ver.startsWith("1.17");
    }
}