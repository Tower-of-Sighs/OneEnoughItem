package com.mafuyu404.oneenoughitem.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GuiUtils {


    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int borderColor) {
        graphics.fill(x - 1, y - 1, x + width + 1, y, borderColor); // 顶边
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, borderColor); // 底边
        graphics.fill(x - 1, y, x, y + height, borderColor); // 左边
        graphics.fill(x + width, y, x + width + 1, y + height, borderColor); // 右边
    }


    private static void drawHighlightBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, 0x80FFFFFF); // 顶亮
        graphics.fill(x, y + height - 1, x + width, y + height, 0x40FFFFFF); // 底亮
    }

    public static void drawPanelBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xB0303030);

        drawBorder(graphics, x, y, width, height, 0xFF202020);

        drawHighlightBorder(graphics, x, y, width, height);
    }

    public static Button createButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return Button.builder(text, onPress)
                .bounds(x, y, width, height)
                .build();
    }


    public static void drawListBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xC0101010);

        drawBorder(graphics, x, y, width, height, 0xFF404040);
    }


    public static void drawFileEntryBackground(GuiGraphics graphics, int x, int y, int width, int height, boolean isHovered, boolean isSelected) {
        if (isSelected) {
            graphics.fill(x, y, x + width, y + height, 0x80404040);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, 0x40FFFFFF);
        }
    }

    public static void drawFileIcon(GuiGraphics graphics, int x, int y, String fileType) {
        graphics.fill(x, y, x + 12, y + 12, 0xFF4CAF50);

        drawBorder(graphics, x, y, 12, 12, 0xFF2E7D32);

        graphics.drawString(Minecraft.getInstance().font, fileType, x + 3, y + 2, 0xFFFFFF);
    }

    public static void drawObjectDropdownBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);

        drawBorder(graphics, x, y, width, height, 0xFF404040);
    }

    public static Button createObjectDropdownButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return new Button(x, y, width, height, text, onPress, Button.DEFAULT_NARRATION) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF000000);

                drawBorder(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFF404040);

                int textColor = this.active ? 0xFFFFFF : 0x808080;
                graphics.drawCenteredString(Minecraft.getInstance().font,
                        this.getMessage(), this.getX() + this.getWidth() / 2,
                        this.getY() + (this.getHeight() - 8) / 2, textColor);
            }
        };
    }
}
