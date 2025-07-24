package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class Utils {
    public static String getItemRegistryName(Item item) {
        if (item == null) {
            return null;
        }
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

            Item item = BuiltInRegistries.ITEM.get(resourceLocation);

            if (item == Items.AIR) {
                Oneenoughitem.LOGGER.debug("getItemById: Item '{}' is null or AIR", registryName);
                return null;
            }
            return item;
        } catch (Exception e) {
            Oneenoughitem.LOGGER.debug("getItemById: Exception while getting item for registry name: {}", registryName, e);
            return null;
        }
    }
}