package com.mafuyu404.oneenoughitem.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

public class TagListWidget extends ObjectSelectionList<TagListWidget.TagEntry> {
    private final Consumer<ResourceLocation> onTagSelect;

    public TagListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, Consumer<ResourceLocation> onTagSelect) {
        super(minecraft, width, height, y, y + height, itemHeight);
        this.onTagSelect = onTagSelect;
    }


    public void setTags(List<ResourceLocation> tags) {
        this.clearEntries();
        for (ResourceLocation tag : tags) {
            this.addEntry(new TagEntry(tag));
        }
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
            graphics.drawString(TagListWidget.this.minecraft.font, tagText, x + 5, y + 5, 0xFFFFFF);
            
            if (isMouseOver) {
                graphics.fill(x, y, x + entryWidth, y + entryHeight, 0x40FFFFFF);
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
    }
}