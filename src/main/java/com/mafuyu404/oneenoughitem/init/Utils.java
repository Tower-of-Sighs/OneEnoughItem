package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Utils {

    public static String getItemRegistryName(Item item) {
        if (item == null) return null;
        ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
        return registryName.toString();
    }

    public static Item getItemById(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            Oneenoughitem.LOGGER.debug("getItemById: registryName is null or empty");
            return null;
        }

        try {
            ResourceLocation resourceLocation = new ResourceLocation(registryName);

            if (!BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
                Oneenoughitem.LOGGER.debug("getItemById: Item '{}' not found in registry", registryName);
                return null;
            }

            return BuiltInRegistries.ITEM.get(resourceLocation);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.debug("getItemById: Exception while getting item for registry name: {}", registryName, e);
            return null;
        }
    }

    public static Collection<Item> getItemsOfTag(ResourceLocation tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        Collection<Item> result = new HashSet<>();

        BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(entryList ->
                entryList.forEach(entry -> result.add(entry.value())));

        return result;
    }

    public static boolean isTagExists(ResourceLocation tagId) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        return BuiltInRegistries.ITEM.getTag(tagKey).isPresent();
    }

    public static List<Item> resolveItemList(List<String> identifiers) {
        List<Item> result = new ArrayList<>();

        for (String id : identifiers) {
            if (id == null || id.isEmpty()) continue;

            if (id.startsWith("#")) {
                String tagStr = id.substring(1);
                ResourceLocation tagId = ResourceLocation.tryParse(tagStr);
                if (tagId == null) {
                    Oneenoughitem.LOGGER.warn("Invalid tag ID format: {}", id);
                    continue;
                }

                Collection<Item> tagItems = getItemsOfTag(tagId);
                if (tagItems.isEmpty()) {
                    Oneenoughitem.LOGGER.warn("Tag {} is empty or not found", tagId);
                } else {
                    result.addAll(tagItems);
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
