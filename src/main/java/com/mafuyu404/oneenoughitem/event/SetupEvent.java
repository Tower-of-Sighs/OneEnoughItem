package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementLoader;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SetupEvent {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(Oneenoughitem.MODID)) Utils.loadAllReplacement();
    }
}
