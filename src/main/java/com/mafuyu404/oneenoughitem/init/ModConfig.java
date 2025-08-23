package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.ServerConfigManager;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;

public class ModConfig extends AutoInitConfigContainer {
    public static final ModConfig INSTANCE = new ModConfig();

    public static final IConfigEntry<Boolean> DEEPER_REPLACE =
            new BooleanEntry("config.oei.common.exp.percent", false).json("Deeper_Replace");
    public static final IConfigEntry<Boolean> CLEAR_FOOD_PROPERTIES =
            new BooleanEntry("config.oei.common.exp.percent", true).json("Clear_Food_Properties");

    public ModConfig() {
        super(new ResourceLocation(Oneenoughitem.MODID, "oei_common_config"), "config.oei.common.title", "./config/oei/common.json");
    }

    @Override
    public void init() {
        this.createTab("common", "config.oei.common.category.common")
                .add(DEEPER_REPLACE)
                .add(CLEAR_FOOD_PROPERTIES);
    }

    public static void register() {
        ConfigManager.getInstance().registerConfigHandler(ModConfig.INSTANCE);
        ServerConfigManager.registerServerConfig(ModConfig.INSTANCE, ServerConfigManager.PermissionChecker.IS_OPERATOR);
    }
}
