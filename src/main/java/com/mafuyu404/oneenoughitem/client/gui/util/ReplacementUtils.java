package com.mafuyu404.oneenoughitem.client.gui.util;

import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ReplacementUtils {
    public static ReplacementInfo getReplacementInfo(String itemId) {
        if (itemId == null) {
            return ReplacementInfo.EMPTY;
        }

        String runtimeReplacement = ReplacementCache.matchItem(itemId);
        if (runtimeReplacement != null) {
            return new ReplacementInfo(true, runtimeReplacement, true, GlobalReplacementCache.isItemUsedAsResult(itemId));
        }

        String globalReplacement = GlobalReplacementCache.getItemReplacement(itemId);
        if (globalReplacement != null) {
            return new ReplacementInfo(true, globalReplacement, false, GlobalReplacementCache.isItemUsedAsResult(itemId));
        }

        return new ReplacementInfo(false, null, false, GlobalReplacementCache.isItemUsedAsResult(itemId));
    }

    public static ReplacementInfo getTagReplacementInfo(ResourceLocation tagId) {
        String runtimeReplacement = ReplacementCache.matchTag(tagId);
        if (runtimeReplacement != null) {
            return new ReplacementInfo(true, runtimeReplacement, true, false);
        }

        String globalReplacement = GlobalReplacementCache.getTagReplacement(tagId.toString());
        if (globalReplacement != null) {
            return new ReplacementInfo(true, globalReplacement, false, false);
        }

        return ReplacementInfo.EMPTY;
    }

    public static class ReplacementInfo {
        public static final ReplacementInfo EMPTY = new ReplacementInfo(false, null, false, false);

        private final boolean isReplaced;
        private final String replacement;
        private final boolean isRuntime;
        private final boolean isUsedAsResult;

        public ReplacementInfo(boolean isReplaced, String replacement, boolean isRuntime, boolean isUsedAsResult) {
            this.isReplaced = isReplaced;
            this.replacement = replacement;
            this.isRuntime = isRuntime;
            this.isUsedAsResult = isUsedAsResult;
        }

        public boolean isReplaced() {
            return isReplaced;
        }

        public boolean isUsedAsResult() {
            return isUsedAsResult;
        }

        public void addToTooltip(List<Component> tooltip) {
            if (isReplaced) {
                tooltip.add(Component.translatable("tooltip.oneenoughitem.item_replaced").withStyle(ChatFormatting.RED));

                if (replacement.startsWith("#")) {
                    tooltip.add(Component.translatable("tooltip.oneenoughitem.replaced_with_tag", replacement).withStyle(ChatFormatting.RED));
                } else {
                    Item replacementItem = Utils.getItemById(replacement);
                    if (replacementItem != null) {
                        tooltip.add(Component.translatable("tooltip.oneenoughitem.replaced_with_item",
                                new ItemStack(replacementItem).getHoverName().getString()).withStyle(ChatFormatting.AQUA));
                    } else {
                        tooltip.add(Component.translatable("tooltip.oneenoughitem.replaced_with_item", replacement).withStyle(ChatFormatting.AQUA));
                    }
                }

                tooltip.add(Component.translatable(isRuntime ?
                                "tooltip.oneenoughitem.source_runtime" : "tooltip.oneenoughitem.source_saved")
                        .withStyle(ChatFormatting.YELLOW));
            }

            if (isUsedAsResult) {
                tooltip.add(Component.translatable("tooltip.oneenoughitem.item_used_as_result").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static class ReplacementIndicator {
        public static void renderItemReplaced(GuiGraphics graphics, int x, int y) {
            graphics.fill(x, y, x + 5, y + 5, 0xFFFF0000);
            graphics.drawString(Minecraft.getInstance().font, "R", x + 1, y, 0xFFFFFFFF, false);
        }

        public static void renderItemUsedAsResult(GuiGraphics graphics, int x, int y) {
            graphics.fill(x, y, x + 5, y + 5, 0xFF00AA00);
            graphics.drawString(Minecraft.getInstance().font, "T", x + 1, y, 0xFFFFFFFF, false);
        }

        public static void renderTagReplaced(GuiGraphics graphics, int x, int y) {
            graphics.fill(x, y, x + 15, y + 15, 0xFFFF0000);
            graphics.drawString(Minecraft.getInstance().font, "R", x + 5, y + 4, 0xFFFFFFFF, false);
        }

        public static void renderTagUsedAsResult(GuiGraphics graphics, int x, int y) {
            graphics.fill(x, y, x + 15, y + 15, 0xFF00AA00);
            graphics.drawString(Minecraft.getInstance().font, "T", x + 5, y + 4, 0xFFFFFFFF, false);
        }
    }
}