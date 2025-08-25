package com.mafuyu404.oneenoughitem.init;

import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.ServerConfigManager;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.interfaces.IConfigEntry;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModConfig extends AutoInitConfigContainer {
    public static final ModConfig INSTANCE = new ModConfig();

    public static final IConfigEntry<Boolean> DEEPER_REPLACE =
            new BooleanEntry("config.oei.common.deeper_replace", false).json("Deeper_Replace");
    public static final IConfigEntry<Boolean> ENABLE_LITE =
            new BooleanEntry("config.oei.common.enable_lite", false).json("Enable_Lite");
    public static final IConfigEntry<Integer> DATA_REWRITE_MODE =
            new IntegerEntry("config.oei.common.data_rewrite_mode", 2, 0, 2).json("Rewrite_Mode");
    public static final IConfigEntry<Integer> TAG_REWRITE_MODE =
            new IntegerEntry("config.oei.common.tag_rewrite_mode", 1, 0, 1).json("Tag_Rewrite_Mode");
    public static final IConfigEntry<List<DirRule>> DATA_DIR_RULES =
            new ListDirRuleEntry(
                    "config.oei.common.data_dir_rules",
                    List.of(
                            new DirRule("recipe", List.of("item", "id", "result"), true),
                            new DirRule("advancement", List.of("item"), false)
                    )
            ).json("Data_Dir_Rules");

    public ModConfig() {
        super(ResourceLocation.fromNamespaceAndPath(Oneenoughitem.MOD_ID, "oei_common_config"), "config.oei.common.title", "./config/oei/common.json");
    }


    @Override
    public void init() {
        this.createTab("common", "config.oei.common.category.common")
                .add(DEEPER_REPLACE)
                .add(ENABLE_LITE)
                .add(DATA_REWRITE_MODE);
        this.createTab("recipe", "config.oei.common.category.recipe")
                .add(DATA_DIR_RULES);
    }

    public static void register() {
        ConfigManager.getInstance().registerConfigHandler(ModConfig.INSTANCE);
        ServerConfigManager.registerServerConfig(ModConfig.INSTANCE, ServerConfigManager.PermissionChecker.IS_OPERATOR);
    }

    public record DirRule(
            String directory,
            List<String> fields,
            boolean deepReplace
    ) {
        public static final Codec<DirRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("directory").forGetter(DirRule::directory),
                Codec.STRING.listOf().fieldOf("fields").forGetter(DirRule::fields),
                Codec.BOOL.fieldOf("deepReplace").forGetter(DirRule::deepReplace)
        ).apply(instance, DirRule::new));
    }
    public static Map<String, MixinUtils.FieldRule> getDirRulesFromConfig() {
        return ModConfig.DATA_DIR_RULES.getValue().stream()
                .collect(Collectors.toMap(
                        ModConfig.DirRule::directory,
                        rule -> new MixinUtils.FieldRule(new HashSet<>(rule.fields()), rule.deepReplace())
                ));
    }

}