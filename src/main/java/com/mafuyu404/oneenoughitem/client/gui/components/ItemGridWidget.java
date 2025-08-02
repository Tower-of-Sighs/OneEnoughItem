package com.mafuyu404.oneenoughitem.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemGridWidget extends AbstractWidget {
    private final int gridWidth;
    private final int gridHeight;
    private final Consumer<ItemStack> onItemClick;
    private List<ItemStack> items = new ArrayList<>();
    private int hoveredIndex = -1;

    public ItemGridWidget(int x, int y, int gridWidth, int gridHeight, Consumer<ItemStack> onItemClick) {
        super(x, y, gridWidth * 18, gridHeight * 18, Component.empty());
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.onItemClick = onItemClick;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < this.items.size() && i < this.gridWidth * this.gridHeight; i++) {
            int row = i / this.gridWidth;
            int col = i % this.gridWidth;
            int itemX = this.getX() + col * 18;
            int itemY = this.getY() + row * 18;

            // Draw slot background
            graphics.fill(itemX, itemY, itemX + 18, itemY + 18, 0xFF8B8B8B);
            graphics.fill(itemX + 1, itemY + 1, itemX + 17, itemY + 17, 0xFF373737);

            // Draw item
            ItemStack itemStack = this.items.get(i);
            graphics.renderItem(itemStack, itemX + 1, itemY + 1);
            graphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, itemX + 1, itemY + 1);

            // Check if hovering and highlight
            if (mouseX >= itemX && mouseX < itemX + 18 && mouseY >= itemY && mouseY < itemY + 18) {
                graphics.fill(itemX + 1, itemY + 1, itemX + 17, itemY + 17, 0x80FFFFFF);
                this.hoveredIndex = i;
            }
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < this.items.size()) {
            ItemStack hoveredStack = this.items.get(this.hoveredIndex);
            if (hoveredStack != null && !hoveredStack.isEmpty()) {
                graphics.renderTooltip(Minecraft.getInstance().font, hoveredStack, mouseX, mouseY);
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

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Item Grid"));
    }
}