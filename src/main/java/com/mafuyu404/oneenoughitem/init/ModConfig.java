package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.ServerConfigManager;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;

public class ModConfig extends AutoInitConfigContainer {
    public static final ModConfig INSTANCE = new ModConfig();

    public static final IConfigEntry<Boolean> DEEPER_REPLACE =
            new BooleanEntry("config.oei.common.deeper_replace", false).json("Deeper_Replace");
    public static final IConfigEntry<Boolean> CLEAR_FOOD_PROPERTIES =
            new BooleanEntry("config.oei.common.clear_food_properties", true).json("Clear_Food_Properties");
    public static final IConfigEntry<Boolean> ENABLE_LITE =
            new BooleanEntry("config.oei.common.enable_lite", false).json("Enable_Lite");
    public static final IConfigEntry<Integer> DATA_REWRITE_MODE =
            new IntegerEntry("config.oei.common.data_rewrite_mode", 2, 0, 2).json("Rewrite_Mode");

    public ModConfig() {
        super(new ResourceLocation(Oneenoughitem.MODID, "oei_common_config"), "config.oei.common.title", "./config/oei/common.json");
    }

    @Override
    public void init() {
        this.createTab("common", "config.oei.common.category.common")
                .add(DEEPER_REPLACE)
                .add(CLEAR_FOOD_PROPERTIES)
                .add(ENABLE_LITE)
                .add(DATA_REWRITE_MODE);
    }

    public static void register() {
        ConfigManager.getInstance().registerConfigHandler(ModConfig.INSTANCE);
        ServerConfigManager.registerServerConfig(ModConfig.INSTANCE, ServerConfigManager.PermissionChecker.IS_OPERATOR);
    }
}