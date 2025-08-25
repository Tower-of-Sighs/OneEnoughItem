package com.mafuyu404.oneenoughitem.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class GuiUtils {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("oneenoughitem", "textures/gui/background.png");
    private static final ResourceLocation BUTTON_NORMAL_TEXTURE = new ResourceLocation("oneenoughitem", "textures/gui/button_0.png");
    private static final ResourceLocation BUTTON_PRESSED_TEXTURE = new ResourceLocation("oneenoughitem", "textures/gui/button_1.png");
    private static final ResourceLocation EDITOR_TEXTURE = new ResourceLocation("oneenoughitem", "textures/gui/editor.png");
    private static final ResourceLocation ITEM_BOX_TEXTURE = new ResourceLocation("oneenoughitem", "textures/gui/item_box.png");


    public static void drawTiledBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        int textureSize = 16;

        for (int tileX = 0; tileX < width; tileX += textureSize) {
            for (int tileY = 0; tileY < height; tileY += textureSize) {
                int tileWidth = Math.min(textureSize, width - tileX);
                int tileHeight = Math.min(textureSize, height - tileY);

                graphics.blit(BACKGROUND_TEXTURE,
                        x + tileX, y + tileY,
                        0, 0,
                        tileWidth, tileHeight,
                        textureSize, textureSize);
            }
        }
    }

    public static void drawItemBox(GuiGraphics graphics, int x, int y, int width, int height) {
        int textureSize = 18;
        int border = 6;
        drawNinePatch(graphics, ITEM_BOX_TEXTURE, x, y, width, height, textureSize, border);
    }

    public static void drawStretchableButton(GuiGraphics graphics, int x, int y, int width, int height, boolean pressed) {
        ResourceLocation texture = pressed ? BUTTON_PRESSED_TEXTURE : BUTTON_NORMAL_TEXTURE;
        int textureSize = 18;
        int border = 6;

        drawNinePatch(graphics, texture, x, y, width, height, textureSize, border);
    }

    private static void drawNinePatch(GuiGraphics graphics, ResourceLocation texture,
                                      int x, int y, int width, int height,
                                      int textureSize, int border) {
        // 角落（不拉伸）
        graphics.blit(texture, x, y, 0, 0, border, border, textureSize, textureSize); // 左上
        graphics.blit(texture, x + width - border, y, textureSize - border, 0, border, border, textureSize, textureSize); // 右上
        graphics.blit(texture, x, y + height - border, 0, textureSize - border, border, border, textureSize, textureSize); // 左下
        graphics.blit(texture, x + width - border, y + height - border, textureSize - border, textureSize - border, border, border, textureSize, textureSize); // 右下

        // 上边（横向拉伸）
        if (width > border * 2) {
            graphics.blit(texture,
                    x + border, y,
                    width - border * 2, border,           // 目标尺寸
                    border, 0,                             // 源UV起点
                    textureSize - border * 2, border,      // 源区尺寸（只取顶部边框带）
                    textureSize, textureSize);
        }

        // 下边（横向拉伸）
        if (width > border * 2) {
            graphics.blit(texture,
                    x + border, y + height - border,
                    width - border * 2, border,
                    border, textureSize - border,
                    textureSize - border * 2, border,
                    textureSize, textureSize);
        }

        // 左边（纵向拉伸）
        if (height > border * 2) {
            graphics.blit(texture,
                    x, y + border,
                    border, height - border * 2,
                    0, border,
                    border, textureSize - border * 2,
                    textureSize, textureSize);
        }

        // 右边（纵向拉伸）
        if (height > border * 2) {
            graphics.blit(texture,
                    x + width - border, y + border,
                    border, height - border * 2,
                    textureSize - border, border,
                    border, textureSize - border * 2,
                    textureSize, textureSize);
        }

        // 中心（双轴拉伸）
        if (width > border * 2 && height > border * 2) {
            graphics.blit(texture,
                    x + border, y + border,
                    width - border * 2, height - border * 2,
                    border, border,
                    textureSize - border * 2, textureSize - border * 2,
                    textureSize, textureSize);
        }
    }

    public static void drawPanelBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        drawTiledBackground(graphics, x, y, width, height);
    }

    public static void drawListBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        drawTiledBackground(graphics, x, y, width, height);
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
        drawTiledBackground(graphics, x, y, width, height);
    }

    public static Button createButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return createCustomButton(text, onPress, x, y, width, height);
    }

    public static Button createObjectDropdownButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return createCustomButton(text, onPress, x, y, width, height);
    }

    private static Button createCustomButton(Component text, Button.OnPress onPress, int x, int y, int width, int height) {
        return new Button(x, y, width, height, text, onPress, Button.DEFAULT_NARRATION) {
            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                renderCustomButton(graphics, this, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                return false;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return super.mouseReleased(mouseX, mouseY, button);
            }
        };
    }

    private static void renderCustomButton(GuiGraphics graphics, Button button, int mouseX, int mouseY, float partialTick) {
        boolean hovered = button.isHovered();
        boolean mouseDown = GLFW.glfwGetMouseButton(
                Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        boolean pressed = hovered && mouseDown;

        drawStretchableButton(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(), pressed);

        int textColor = button.active ? (hovered ? 0xFFFFA0 : 0xFFFFFF) : 0x808080;
        graphics.drawCenteredString(Minecraft.getInstance().font,
                button.getMessage(), button.getX() + button.getWidth() / 2,
                button.getY() + (button.getHeight() - 8) / 2, textColor);
    }

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

    public static void drawEditorPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        int textureSize = 256;
        int border = 20;
        drawNinePatch(graphics, EDITOR_TEXTURE, x, y, width, height, textureSize, border);
    }
}