package com.mafuyu404.oneenoughitem.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class Utils {
    public static String toPathString(String key) {
        String[] path = key.split("\\.");
        return path[1] + ":" + path[2];
    }

    public static Item getItemById(String registryName) {
        try {
            ResourceLocation location = new ResourceLocation(registryName);
            return BuiltInRegistries.ITEM.get(location);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getItemRegistryName(Item item) {
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(item);
        return location != null ? location.toString() : "unknown";
    }
}