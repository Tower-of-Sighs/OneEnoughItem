package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.client.gui.components.TagListWidget;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TagSelectionScreen extends Screen {
    private final ReplacementEditorScreen parent;
    private final boolean isForMatch;

    private EditBox searchBox;
    private Button backButton;
    private TagListWidget tagList;

    private List<ResourceLocation> allTags;
    private List<ResourceLocation> filteredTags;

    public TagSelectionScreen(ReplacementEditorScreen parent, boolean isForMatch) {
        super(Component.translatable("gui.oneenoughitem.tag_selection.title"));
        this.parent = parent;
        this.isForMatch = isForMatch;
        this.initializeTags();
    }

    private void initializeTags() {
        this.allTags = ForgeRegistries.ITEMS.tags().getTagNames()
                .map(TagKey::location)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .collect(Collectors.toList());
        this.filteredTags = new ArrayList<>(this.allTags);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        this.searchBox = new EditBox(this.font, centerX - 80, 15, 160, 18,
                Component.translatable("gui.oneenoughitem.search"));
        this.searchBox.setHint(Component.translatable("gui.oneenoughitem.search_tags.hint"));
        this.addRenderableWidget(this.searchBox);

        this.tagList = new TagListWidget(this.minecraft, this.width - 40, this.height - 100, 40, 22, this::selectTag);
        this.tagList.setLeftPos(20);
        this.addRenderableWidget(this.tagList);

        this.backButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.back"),
                btn -> this.onClose(), centerX - 40, this.height - 50, 80, 18);
        this.addRenderableWidget(this.backButton);

        this.updateTagList();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.searchBox.getValue().length() != this.getLastSearchLength()) {
            this.filterTags();
            this.updateTagList();
        }
    }

    private int lastSearchLength = 0;

    private int getLastSearchLength() {
        int current = this.lastSearchLength;
        this.lastSearchLength = this.searchBox.getValue().length();
        return current;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        int panelX = 20;
        int panelY = 40;
        int panelW = this.width - 40;
        int panelH = this.height - 100;
        GuiUtils.drawPanelBackground(graphics, panelX, panelY, panelW, panelH);

        super.render(graphics, mouseX, mouseY, partialTick);

        String tagCount = this.filteredTags.size() + " tags";
        graphics.drawString(this.font, tagCount, 10, this.height - 30, 0xFFFFFF);

        TagListWidget.TagEntry hoveredEntry = this.tagList.getEntryAtMouse(mouseX, mouseY);
        if (hoveredEntry != null) {
            List<Component> tooltip = hoveredEntry.getTooltip();
            graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX, mouseY);
        }
    }

    private void filterTags() {
        String search = this.searchBox.getValue().toLowerCase();
        if (search.isEmpty()) {
            this.filteredTags = new ArrayList<>(this.allTags);
        } else {
            this.filteredTags = this.allTags.stream()
                    .filter(tag -> tag.toString().toLowerCase().contains(search))
                    .collect(Collectors.toList());
        }
    }

    private void updateTagList() {
        this.tagList.setTags(this.filteredTags);
    }

    private void selectTag(ResourceLocation tagId) {
        if (this.isForMatch) {
            this.parent.addMatchTag(tagId);
        } else {
            this.parent.setResultTag(tagId);
        }
        this.onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}