package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.client.gui.util.ReplacementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TagListWidget extends ObjectSelectionList<TagListWidget.TagEntry> {
    private final Consumer<ResourceLocation> onTagSelect;

    public TagListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, Consumer<ResourceLocation> onTagSelect) {
        super(minecraft, width, height, y, y + height, itemHeight);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.onTagSelect = onTagSelect;
    }

    public void setTags(List<ResourceLocation> tags) {
        this.clearEntries();
        for (ResourceLocation tag : tags) {
            this.addEntry(new TagEntry(tag));
        }
    }

    public TagEntry getEntryAtMouse(double mouseX, double mouseY) {
        if (mouseX >= this.x0 && mouseX <= this.x0 + this.width &&
                mouseY >= this.y0 && mouseY <= this.y0 + this.height) {

            int relativeY = (int) (mouseY - this.y0 + this.getScrollAmount());
            int index = relativeY / this.itemHeight;

            if (index >= 0 && index < this.children().size()) {
                return this.children().get(index);
            }
        }
        return null;
    }

    public class TagEntry extends ObjectSelectionList.Entry<TagEntry> {
        private final ResourceLocation tagId;

        public TagEntry(ResourceLocation tagId) {
            this.tagId = tagId;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            String tagText = "#" + this.tagId.toString();
            ReplacementUtils.ReplacementInfo replacementInfo = ReplacementUtils.getTagReplacementInfo(this.tagId);

            int textColor = replacementInfo.isReplaced() ? 0xFFFF5555 : 0xFFFFFFFF;
            if (isMouseOver && !replacementInfo.isReplaced()) {
                textColor = 0xFFFFFF88;
            }

            graphics.drawString(TagListWidget.this.minecraft.font, tagText, x + 5, y + 5, textColor);

            if (replacementInfo.isReplaced()) {
                ReplacementUtils.ReplacementIndicator.renderTagReplaced(graphics, x + entryWidth - 20, y + 2);
            }
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            TagListWidget.this.onTagSelect.accept(this.tagId);
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal("#" + this.tagId.toString());
        }

        public List<Component> getTooltip() {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("#" + this.tagId.toString()));

            ReplacementUtils.ReplacementInfo replacementInfo = ReplacementUtils.getTagReplacementInfo(this.tagId);
            replacementInfo.addToTooltip(tooltip);

            tooltip.add(Component.literal(this.tagId.getNamespace()).withStyle(ChatFormatting.BLUE));
            return tooltip;
        }
    }
}