package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Utils {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }

        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
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

            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);

//            if (item == null || item == Items.AIR) {
//                Oneenoughitem.LOGGER.debug("getItemById: Item '{}' is null or AIR", registryName);
//                return null;
//            }
            return item;
        } catch (Exception e) {
            Oneenoughitem.LOGGER.debug("getItemById: Exception while getting item for registry name: {}", registryName, e);
            return null;
        }
    }

    public static Collection<Item> getItemsOfTag(ResourceLocation tagId) {
        TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        Collection<Item> result = new HashSet<>();

        if (tagManager != null && tagManager.isKnownTagName(tagKey)) {
            tagManager.getTag(tagKey).forEach(result::add);
        }
        return result;
    }

    public static boolean isTagExists(ResourceLocation tagId) {
        TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(tagId);
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        return tagManager != null && tagManager.isKnownTagName(tagKey);
    }

    public static List<Item> resolveItemList(List<String> identifiers) {
        List<Item> result = new ArrayList<>();

        for (String id : identifiers) {
            if (id == null || id.isEmpty()) continue;

            if (id.startsWith("#")) {
                String tagIdString = id.substring(1);
                try {
                    ResourceLocation tagId = new ResourceLocation(tagIdString);
                    Collection<Item> tagItems = getItemsOfTag(tagId);
                    if (tagItems.isEmpty()) {
                        Oneenoughitem.LOGGER.warn("Tag {} is empty or not found", tagId);
                    } else {
                        result.addAll(tagItems);
                        Oneenoughitem.LOGGER.debug("Resolved tag {} to {} items", tagId, tagItems.size());
                    }
                } catch (Exception e) {
                    Oneenoughitem.LOGGER.error("Invalid tag ID format: {}", id, e);
                }
            } else {
                Item item = getItemById(id);
                if (item != null) {
                    result.add(item);
                } else {
                    Oneenoughitem.LOGGER.warn("Item ID not found: {}", id);
                }
            }
        }

        return result;
    }
}