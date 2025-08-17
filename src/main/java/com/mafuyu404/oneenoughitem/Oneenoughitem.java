package com.mafuyu404.oneenoughitem;

import com.mafuyu404.oelib.neoforge.data.DataRegistry;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Oneenoughitem.MOD_ID)
public class Oneenoughitem {
    public static final String MOD_ID = "oneenoughitem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Oneenoughitem(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        DataRegistry.register(Replacements.class);
    }

    public static ResourceLocation ResourceLocationMod(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
