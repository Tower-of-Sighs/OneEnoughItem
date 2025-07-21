package com.mafuyu404.oneenoughitem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemFetcher {
    public static Item getItemById(String registryName) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(registryName));
    }
}
