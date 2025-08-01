package com.mafuyu404.oneenoughitem.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemDisplayWidget extends AbstractWidget {
    private final ItemStack itemStack;
    private final Button.OnPress removeAction;
    private final Font font = Minecraft.getInstance().font;

    public ItemDisplayWidget(int x, int y, ItemStack itemStack, Button.OnPress removeAction) {
        super(x, y, 18, 18, Component.empty());
        this.itemStack = itemStack;
        this.removeAction = removeAction;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX(), this.getY(), this.getX() + 18, this.getY() + 18, 0xFF8B8B8B);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + 17, this.getY() + 17, 0xFF373737);

        graphics.renderItem(this.itemStack, this.getX() + 1, this.getY() + 1);

        if (this.isHovered() && this.removeAction != null) {
            graphics.fill(this.getX() + 12, this.getY() - 2, this.getX() + 20, this.getY() + 6, 0xFFFF0000);
            graphics.drawString(font, "×", this.getX() + 14, this.getY(), 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered() && this.removeAction != null) {
            if (mouseX >= this.getX() + 12 && mouseX <= this.getX() + 20 &&
                mouseY >= this.getY() - 2 && mouseY <= this.getY() + 6) {
                this.removeAction.onPress(null);
                return true;
            }
        }
        return false;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.itemStack.getHoverName());
    }
}