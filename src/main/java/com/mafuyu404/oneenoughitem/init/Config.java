package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DATA_DIR_RULES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> DEEPER_REPLACE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CLEAR_FOOD_PROPERTIES;
    public static final ForgeConfigSpec.ConfigValue<Boolean> MODERNFIX_WARNING_SHOWN;
    public static final ForgeConfigSpec.ConfigValue<Integer> DATA_REWRITE_MODE ;
    public static final ForgeConfigSpec.ConfigValue<Integer> TAG_REWRITE_MODE ;

    static {
        BUILDER.push("Common Setting");

        DEEPER_REPLACE = BUILDER
                .comment("For example, now you can heal iron golem with eggs that replaced iron ingot.")
                .define("DeeperReplace", false);

        CLEAR_FOOD_PROPERTIES = BUILDER
                .comment("There won't any items in food list if items were replaced.")
                .define("ClearFoodProperties", true);

        MODERNFIX_WARNING_SHOWN = BUILDER
                .comment("Internal flag to track if the ModernFix warning has been shown. Do not modify manually.(You don't need to modify it)")
                .define("modernfixWarningShown", false);

        DATA_REWRITE_MODE = BUILDER
                .comment(
                        "Controls the replacement mode for data pack JSONs, excluding tags.\n" +
                                "The data pack scope is determined by the DATA_DIR_RULES configuration (e.g., recipes, advancements, etc.).\n" +
                                "0 = No changes, 1 = Keep all data, 2 = Remove all data related to replaced items."
                )
                .defineInRange("DataRewriteMode", 2, 0, 2);

        TAG_REWRITE_MODE = BUILDER
                .comment(
                        "Controls the replacement mode for tags, affecting only item tags.\n" +
                                "0 = No changes, 1 = Remove all item tags related to replaced items."
                )
                .defineInRange("TagRewriteMode", 1, 0, 1);

        BUILDER.pop();



        BUILDER.push("Recipe Settings");

        DATA_DIR_RULES = BUILDER
                .comment(
                        "Specifies which data directories should be checked and rewritten (e.g., recipes, advancements, etc.).\n" +
                                "Each directory can define which fields to check and whether deep replacement should be applied.\n" +
                                "Example: recipes -> {fields: [\"item\",\"id\",\"result\"], deepReplace: true}"
                )
                .define("DataDirRules", List.of(
                        "recipes,item|id|result,true",
                        "advancements,item,false"
                ));

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static Map<String, MixinUtils.FieldRule> parseDirRules() {
        Map<String, MixinUtils.FieldRule> map = new HashMap<>();
        for (String s : DATA_DIR_RULES.get()) {
            try {
                String[] parts = s.split(",");
                if (parts.length < 3) continue;
                String dir = parts[0].trim();
                Set<String> fields = new HashSet<>(Arrays.asList(parts[1].trim().split("\\|")));
                boolean deepReplace = Boolean.parseBoolean(parts[2].trim());
                map.put(dir, new MixinUtils.FieldRule(fields, deepReplace));
            } catch (Exception e) {
                Oneenoughitem.LOGGER.warn("Invalid DirRule config: {}", s, e);
            }
        }
        return map;
    }
}
