package com.mafuyu404.oneenoughitem;

import com.mafuyu404.oelib.forge.data.DataRegistry;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.config.Config;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Oneenoughitem.MODID)
public class Oneenoughitem {
    public static final String MODID = "oneenoughitem";
    public static final Logger LOGGER = LogManager.getLogger();

    public Oneenoughitem() {
        Config.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        DataRegistry.register(Replacements.class);

        LOGGER.info("OneEnoughItem initialized with OELib data-driven framework");
    }
}