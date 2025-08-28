package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import com.mafuyu404.oneenoughitem.client.gui.util.ReplacementUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ItemGridWidget extends AbstractWidget {
    private final int gridWidth;
    private final int gridHeight;
    private final Consumer<ItemStack> onItemClick;
    private List<ItemStack> items = new ArrayList<>();
    private int hoveredIndex = -1;

    private Set<String> selectedItemIds = Collections.emptySet();

    public ItemGridWidget(int x, int y, int gridWidth, int gridHeight, Consumer<ItemStack> onItemClick) {
        super(x, y, gridWidth * 18, gridHeight * 18, Component.empty());
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.onItemClick = onItemClick;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredIndex = -1;

        for (int i = 0; i < this.items.size() && i < this.gridWidth * 18 / 18 * this.gridHeight; i++) {
            int row = i / this.gridWidth;
            int col = i % this.gridWidth;
            int itemX = this.getX() + col * 18;
            int itemY = this.getY() + row * 18;

            GuiUtils.drawItemBox(graphics, itemX, itemY, 18, 18);

            ItemStack itemStack = this.items.get(i);
            ReplacementControl.withSkipReplacement(() -> {
                graphics.renderItem(itemStack, itemX + 1, itemY + 1);
                graphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, itemX + 1, itemY + 1);
            });

            String itemId = Utils.getItemRegistryName(itemStack.getItem());
            ReplacementUtils.ReplacementInfo replacementInfo = ReplacementUtils.getReplacementInfo(itemId);

            if (replacementInfo.isReplaced()) {
                ReplacementUtils.ReplacementIndicator.renderItemReplaced(graphics, itemX + 12, itemY + 1);
            } else if (replacementInfo.isUsedAsResult()) {
                ReplacementUtils.ReplacementIndicator.renderItemUsedAsResult(graphics, itemX + 12, itemY + 1);
            }

            if (itemId != null && this.selectedItemIds.contains(itemId)) {
                graphics.fill(itemX + 1, itemY + 1, itemX + 17, itemY + 17, 0x8033AAFF);
            }
            if (mouseX >= itemX && mouseX < itemX + 18 && mouseY >= itemY && mouseY < itemY + 18) {
                graphics.fill(itemX + 1, itemY + 1, itemX + 17, itemY + 17, 0x80FFFFFF);
                this.hoveredIndex = i;
            }
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.items.size()) {
            ItemStack hoveredStack = this.items.get(this.hoveredIndex);
            if (hoveredStack != null && !hoveredStack.isEmpty()) {
                List<Component> tooltip = new ArrayList<>();

                ReplacementControl.withSkipReplacement(() -> {
                    tooltip.add(hoveredStack.getHoverName());
                });

                String itemId = Utils.getItemRegistryName(hoveredStack.getItem());
                if (itemId != null) {
                    ReplacementUtils.ReplacementInfo replacementInfo = ReplacementUtils.getReplacementInfo(itemId);
                    replacementInfo.addToTooltip(tooltip);
                }

                String modId = BuiltInRegistries.ITEM.getKey(hoveredStack.getItem()).getNamespace();
                tooltip.add(Component.literal(modId).withStyle(ChatFormatting.BLUE));

                graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered()) {
            int relativeX = (int) (mouseX - this.getX());
            int relativeY = (int) (mouseY - this.getY());
            int col = relativeX / 18;
            int row = relativeY / 18;
            int index = row * this.gridWidth + col;

            if (index >= 0 && index < this.items.size()) {
                this.onItemClick.accept(this.items.get(index));
                return true;
            }
        }
        return false;
    }

    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>(items);
    }

    public void setSelectedItemIds(Set<String> selectedItemIds) {
        this.selectedItemIds = selectedItemIds != null ? selectedItemIds : Collections.emptySet();
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Item Grid"));
    }
}
