package com.mafuyu404.oneenoughitem.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ScrollablePanel extends AbstractWidget {
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int maxVisibleHeight;
    private boolean isDragging = false;
    private final int scrollbarWidth = 6;
    private int scrollbarHeight = 0;
    private int scrollbarY = 0;

    public ScrollablePanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.maxVisibleHeight = height;
    }

    public void addWidget(AbstractWidget widget) {
        this.widgets.add(widget);
        this.updateContentHeight();
    }

    public void clearWidgets() {
        this.widgets.clear();
        this.scrollOffset = 0;
        this.contentHeight = 0;
    }

    private void updateContentHeight() {
        int maxY = 0;
        for (AbstractWidget widget : this.widgets) {
            int relativeY = widget.getY() + widget.getHeight() - this.getY();
            maxY = Math.max(maxY, relativeY);
        }
        this.contentHeight = maxY;

        // Update scrollbar height
        if (this.contentHeight > this.maxVisibleHeight) {
            this.scrollbarHeight = Math.max(20, (this.maxVisibleHeight * this.maxVisibleHeight) / this.contentHeight);
        } else {
            this.scrollbarHeight = 0;
        }

        int maxScroll = Math.max(0, this.contentHeight - this.maxVisibleHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.maxVisibleHeight);

        AbstractWidget hoveredChild = null;
        for (AbstractWidget widget : this.widgets) {
            int widgetY = widget.getY() - this.scrollOffset;
            if (widgetY + widget.getHeight() >= this.getY() && widgetY <= this.getY() + this.maxVisibleHeight) {
                int originalY = widget.getY();
                widget.setY(widgetY);
                widget.render(graphics, mouseX, mouseY, partialTick);

                if (mouseY >= widgetY && mouseY <= widgetY + widget.getHeight() &&
                        mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()) {
                    hoveredChild = widget;
                }
                widget.setY(originalY);
            }
        }

        graphics.disableScissor();

        if (hoveredChild instanceof ItemDisplayWidget idw) {
            idw.renderTooltip(graphics, mouseX, mouseY);
        }

        if (this.contentHeight > this.maxVisibleHeight) {
            this.drawScrollbar(graphics);
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int scrollbarX = this.getX() + this.width - this.scrollbarWidth - 33;
        int trackHeight = this.maxVisibleHeight;

        graphics.fill(scrollbarX, this.getY(), scrollbarX + this.scrollbarWidth, this.getY() + trackHeight, 0x30FFFFFF);

        int maxScroll = Math.max(0, this.contentHeight - this.maxVisibleHeight);
        if (maxScroll > 0) {
            float scrollRatio = (float) this.scrollOffset / maxScroll;
            this.scrollbarY = this.getY() + (int) (scrollRatio * (trackHeight - this.scrollbarHeight));

            graphics.fill(scrollbarX, this.scrollbarY, scrollbarX + this.scrollbarWidth,
                    this.scrollbarY + this.scrollbarHeight, 0x50FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isHovered()) return false;

        if (this.contentHeight > this.maxVisibleHeight) {
            int scrollbarX = this.getX() + this.width - this.scrollbarWidth;
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + this.scrollbarWidth) {
                if (mouseY >= this.scrollbarY && mouseY <= this.scrollbarY + this.scrollbarHeight) {
                    this.isDragging = true;
                    return true;
                }
            }
        }

        for (AbstractWidget widget : this.widgets) {
            int widgetY = widget.getY() - this.scrollOffset;
            // 检查鼠标是否在widget的渲染位置内
            if (mouseY >= widgetY && mouseY <= widgetY + widget.getHeight() &&
                    mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()) {

                // 临时调整widget位置进行点击检测
                int originalY = widget.getY();
                widget.setY(widgetY);
                boolean result = widget.mouseClicked(mouseX, mouseY, button);
                widget.setY(originalY); // 恢复原始位置

                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDragging = false;

        // 处理子组件的鼠标释放事件
        for (AbstractWidget widget : this.widgets) {
            int widgetY = widget.getY() - this.scrollOffset;
            if (mouseY >= widgetY && mouseY <= widgetY + widget.getHeight() &&
                    mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()) {

                int originalY = widget.getY();
                widget.setY(widgetY);
                boolean result = widget.mouseReleased(mouseX, mouseY, button);
                widget.setY(originalY);

                if (result) {
                    return true;
                }
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging && this.contentHeight > this.maxVisibleHeight) {
            int trackHeight = this.maxVisibleHeight - this.scrollbarHeight;
            int maxScroll = Math.max(0, this.contentHeight - this.maxVisibleHeight);

            double scrollRatio = (mouseY - this.getY()) / trackHeight;
            this.scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollRatio * maxScroll));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.isHovered() && this.contentHeight > this.maxVisibleHeight) {
            int maxScroll = Math.max(0, this.contentHeight - this.maxVisibleHeight);
            this.scrollOffset = (int) Math.max(0, Math.min(maxScroll, this.scrollOffset - delta * 10));
            return true;
        }
        return false;
    }

    public void updateWidgetPositions() {
        this.updateContentHeight();
    }

    public void setVisibleHeight(int height) {
        this.maxVisibleHeight = Math.max(0, height);
        this.height = this.maxVisibleHeight;
        this.updateContentHeight();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Scrollable Panel"));
    }
}