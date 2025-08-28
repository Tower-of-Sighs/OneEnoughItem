package com.mafuyu404.oneenoughitem.init.config;

import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.ServerConfigManager;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public class ModConfig extends AutoInitConfigContainer {
    public static final ModConfig INSTANCE = new ModConfig();

    public static final IConfigEntry<Boolean> DEEPER_REPLACE =
            new BooleanEntry("config.oei.common.deeper_replace", false).json("Deeper_Replace");
    public static final IConfigEntry<Boolean> ENABLE_LITE =
            new BooleanEntry("config.oei.common.enable_lite", false).json("Enable_Lite");
    public static final IConfigEntry<Boolean> CLEAR_FOOD_PROPERTIES =
            new BooleanEntry("config.oei.common.clear_food_properties", false).json("Clear_Food_Properties");
    public static final IConfigEntry<DefaultRules> DEFAULT_RULES =
            new DefaultRulesEntry(
                    "config.oei.common.default_rules",
                    new DefaultRules(Optional.empty(), Optional.empty()),
                    DefaultRules.CODEC
            ).json("Default_Rules");

    public ModConfig() {
        super(new ResourceLocation(Oneenoughitem.MODID, "oei_common_config"), "config.oei.common.title", "./config/oei/common.json");
    }


    @Override
    public void init() {
        this.createTab("common", "config.oei.common.category.common")
                .add(DEEPER_REPLACE)
                .add(CLEAR_FOOD_PROPERTIES)
                .add(ENABLE_LITE)
                .add(DEFAULT_RULES);
    }

    public static void register() {
        ConfigManager.getInstance().registerConfigHandler(ModConfig.INSTANCE);
        ServerConfigManager.registerServerConfig(ModConfig.INSTANCE, ServerConfigManager.PermissionChecker.IS_OPERATOR);
    }

    public record DefaultRules(
            Optional<Map<String, Replacements.ProcessingMode>> data,
            Optional<Map<String, Replacements.ProcessingMode>> tag
    ) {
        public static final Codec<DefaultRules> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.unboundedMap(Codec.STRING, Replacements.ProcessingMode.CODEC)
                                .optionalFieldOf("data")
                                .forGetter(DefaultRules::data),
                        Codec.unboundedMap(Codec.STRING, Replacements.ProcessingMode.CODEC)
                                .optionalFieldOf("tag")
                                .forGetter(DefaultRules::tag)
                ).apply(instance, DefaultRules::new)
        );

        public Replacements.Rules toRules() {
            return new Replacements.Rules(this.data, this.tag);
        }
    }
}