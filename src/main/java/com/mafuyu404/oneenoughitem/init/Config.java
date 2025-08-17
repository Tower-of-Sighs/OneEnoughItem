package com.mafuyu404.oneenoughitem.init;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Oneenoughitem.MOD_ID)
public class Config {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEEPER_REPLACE;
    public static final ModConfigSpec.BooleanValue MODERNFIX_WARNING_SHOWN;

    public static boolean IS_DEEPER_REPLACE_ENABLED;
    public static boolean IS_MODERNFIX_WARNING_SHOWN;
    public static volatile boolean CONFIG_LOADED = false;

    static {
        BUILDER.push("OEI Setting");

        DEEPER_REPLACE = BUILDER
                .comment("For example, now you can heal iron golem with eggs that replaced iron ingot.")
                .define("DeeperReplace", false);

        MODERNFIX_WARNING_SHOWN = BUILDER
                .comment("Internal flag to track if the ModernFix warning has been shown. Do not modify manually.")
                .define("modernfixWarningShown", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    public static void onConfigLoading(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            IS_DEEPER_REPLACE_ENABLED = DEEPER_REPLACE.get();
            IS_MODERNFIX_WARNING_SHOWN = MODERNFIX_WARNING_SHOWN.get();
            CONFIG_LOADED = true;
            Oneenoughitem.LOGGER.info("Config loaded. DEEPER_REPLACE={}, MODERNFIX_WARNING_SHOWN={}",
                    IS_DEEPER_REPLACE_ENABLED, IS_MODERNFIX_WARNING_SHOWN);
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            IS_DEEPER_REPLACE_ENABLED = DEEPER_REPLACE.get();
            IS_MODERNFIX_WARNING_SHOWN = MODERNFIX_WARNING_SHOWN.get();
            CONFIG_LOADED = true;
            Oneenoughitem.LOGGER.info("Config reloaded. DEEPER_REPLACE={}, MODERNFIX_WARNING_SHOWN={}",
                    IS_DEEPER_REPLACE_ENABLED, IS_MODERNFIX_WARNING_SHOWN);
        }
    }
}