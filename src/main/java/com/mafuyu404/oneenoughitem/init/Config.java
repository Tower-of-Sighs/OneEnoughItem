package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    //    public static final ForgeConfigSpec.ConfigValue<Boolean> BREAK_INVALID_TAGS;
    public static final ForgeConfigSpec.ConfigValue<Boolean> DEEPER_REPLACE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CLEAR_FOOD_PROPERTIES;

    static {
        BUILDER.push("OEI Setting");

//        BREAK_INVALID_TAGS = BUILDER
//                .comment("There won't any items in tags if items were replaced.")
//                .define("BreakInvalidTags", false);
        DEEPER_REPLACE = BUILDER
                .comment("For example, now you can heal iron golem with eggs that replaced iron ingot.")
                .define("DeeperReplace", false);
        CLEAR_FOOD_PROPERTIES = BUILDER
                .comment("There won't any items in food list if items were replaced.")
                .define("ClearFoodProperties", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
