package com.mafuyu404.oneenoughitem.event;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.util.ModernFixDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Oneenoughitem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (ModernFixDetector.shouldShowWarning()) {
                ModernFixDetector.markWarningShown();

                MutableComponent line1 = Component.translatable("oneenoughitem.modernfix.warning.line1")
                        .withStyle(ChatFormatting.AQUA);

                MutableComponent line2Start = Component.translatable("oneenoughitem.modernfix.warning.line2")
                        .withStyle(ChatFormatting.AQUA);

                MutableComponent clickableLink = Component.translatable("oneenoughitem.modernfix.warning.link")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, ModernFixDetector.getConfigPath().toString()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("oneenoughitem.modernfix.warning.hover", ModernFixDetector.getConfigPath().toString())
                                                .withStyle(ChatFormatting.GRAY))));

                MutableComponent line2End = Component.translatable("oneenoughitem.modernfix.warning.suffix")
                        .withStyle(ChatFormatting.AQUA);

                MutableComponent line2 = line2Start.append(clickableLink).append(line2End);

                player.sendSystemMessage(line1);
                player.sendSystemMessage(line2);
            }
        }
    }
}