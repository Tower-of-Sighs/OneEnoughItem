package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TagDisplayWidget extends AbstractWidget {
    private final ResourceLocation tagId;
    private final Button.OnPress removeAction;
    private final Font font = Minecraft.getInstance().font;

    private long hoverStartTime = 0;
    private int scrollOffset = 0;
    private boolean isScrolling = false;
    private static final int SCROLL_DELAY = 250; // 悬停0.5秒后开始滚动（要不然有点违和）
    private long lastScrollTime = 0;
    private static final int SCROLL_INTERVAL = 60;
    private static final ResourceLocation ITEM_BOX_TEX = new ResourceLocation(Oneenoughitem.MODID, "textures/gui/item_box.png");
    private static final ResourceLocation CROSS_TEX = new ResourceLocation(Oneenoughitem.MODID, "textures/gui/cross.png");

    public TagDisplayWidget(int x, int y, ResourceLocation tagId, Button.OnPress removeAction) {
        super(x, y, 70, 20, Component.empty());
        this.tagId = tagId;
        this.removeAction = removeAction;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.drawItemBox(graphics, this.getX(), this.getY(), this.width, this.height);

        String fullTagText = "#" + this.tagId.toString();
        String displayText = fullTagText;
        int textX = this.getX() + 3;
        if (this.isHovered()) {
            if (this.hoverStartTime == 0) {
                this.hoverStartTime = System.currentTimeMillis();
                this.scrollOffset = 0;
                this.isScrolling = false;
            }

            long hoverDuration = System.currentTimeMillis() - this.hoverStartTime;
            int textWidth = this.font.width(fullTagText);
            int availableWidth = this.width - 6 - (this.removeAction != null ? 12 : 0); // 减去边距和删除按钮空间

            if (textWidth > availableWidth && hoverDuration > SCROLL_DELAY) {
                this.isScrolling = true;

                int maxScroll = textWidth - availableWidth + 20;

                long now = System.currentTimeMillis();
                if (now - lastScrollTime > SCROLL_INTERVAL) {
                    this.scrollOffset += 1;
                    this.lastScrollTime = now;
                }

                if (this.scrollOffset > maxScroll) {
                    this.scrollOffset = -50; // 留点间隙再重新开始
                }

                textX = this.getX() + 3 - this.scrollOffset;
            } else if (textWidth > availableWidth) {
                // 悬停但还未开始滚动，显示截断文本
                displayText = this.truncateText(fullTagText, availableWidth);
            }
        } else {
            // 不悬停时重置滚动状态
            this.hoverStartTime = 0;
            this.scrollOffset = 0;
            this.isScrolling = false;

            // 显示截断文本
            int availableWidth = this.width - 6 - (this.removeAction != null ? 12 : 0);
            if (this.font.width(fullTagText) > availableWidth) {
                displayText = this.truncateText(fullTagText, availableWidth);
            }
        }

        // 设置裁剪区域以防止文本溢出
        if (this.isScrolling) {
            int clipX = this.getX() + 3;
            int clipY = this.getY();
            int clipWidth = this.width - 6 - (this.removeAction != null ? 12 : 0);
            int clipHeight = this.height;

            graphics.enableScissor(clipX, clipY, clipX + clipWidth, clipY + clipHeight);
        }

        graphics.drawString(font, displayText, textX, this.getY() + 6, 0xFFFFFFFF, false);

        if (this.isScrolling) {
            graphics.disableScissor();
        }

        if (this.isHovered() && this.removeAction != null) {
            int crossX = this.getX() + this.width - 9;
            int crossY = this.getY() + 1;
            graphics.blit(CROSS_TEX, crossX, crossY, 0, 0, 8, 8, 8, 8);
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String truncated = text;
        while (this.font.width(truncated + "...") > maxWidth && truncated.length() > 1) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + "...";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered() && this.removeAction != null) {
            int crossX = this.getX() + this.width - 9;
            int crossY = this.getY() + 1;
            if (mouseX >= crossX && mouseX <= crossX + 8 &&
                    mouseY >= crossY && mouseY <= crossY + 8) {
                this.removeAction.onPress(null);
                return true;
            }
        }
        return false;
    }

    public ResourceLocation getTagId() {
        return this.tagId;
    }

    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("#" + this.tagId.toString()));
    }
}