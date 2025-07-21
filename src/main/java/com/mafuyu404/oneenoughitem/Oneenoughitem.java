package com.mafuyu404.oneenoughitem;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Oneenoughitem.MODID)
public class Oneenoughitem {
    public static final String MODID = "oneenoughitem";

    public Oneenoughitem() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
