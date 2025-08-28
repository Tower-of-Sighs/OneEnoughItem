package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.client.gui.components.TagListWidget;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.*;
import java.util.stream.Collectors;

public class TagSelectionScreen extends Screen {
    private final ReplacementEditorScreen parent;
    private final boolean isForMatch;

    private EditBox searchBox;
    private Button backButton;
    private Button confirmSelectionButton;
    private Button clearSelectionButton;

    private TagListWidget tagList;

    private List<ResourceLocation> allTags;
    private List<ResourceLocation> filteredTags;
    private final Set<ResourceLocation> selectedTags = new HashSet<>();

    public TagSelectionScreen(ReplacementEditorScreen parent, boolean isForMatch) {
        super(Component.translatable("gui.oneenoughitem.tag_selection.title"));
        this.parent = parent;
        this.isForMatch = isForMatch;
        this.initializeTags();
    }

    private void initializeTags() {
        this.allTags = BuiltInRegistries.ITEM.getTagNames()
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
        this.tagList.setX(20);
        this.tagList.setSelectedTags(this.selectedTags);
        this.addRenderableWidget(this.tagList);
        if (this.isForMatch) {
            int buttonY = this.height - 50;

            int totalWidth = 100 + 80 + 100 + 10;
            int startX = centerX - totalWidth / 2;

            this.confirmSelectionButton = GuiUtils.createButton(
                    Component.translatable("gui.oneenoughitem.add_selected"),
                    btn -> this.confirmSelectedTags(),
                    startX, buttonY, 100, 18
            );
            this.addRenderableWidget(this.confirmSelectionButton);

            this.backButton = GuiUtils.createButton(
                    Component.translatable("gui.oneenoughitem.back"),
                    btn -> this.onClose(),
                    startX + 100 + 10, buttonY, 80, 18
            );
            this.addRenderableWidget(this.backButton);

            this.clearSelectionButton = GuiUtils.createButton(
                    Component.translatable("gui.oneenoughitem.clear_selected"),
                    btn -> {
                        this.selectedTags.clear();
                        this.updateTagList();
                        this.updateConfirmButtonsVisibility();
                    },
                    startX + 100 + 10 + 80 + 10, buttonY, 100, 18
            );
            this.addRenderableWidget(this.clearSelectionButton);

            this.updateConfirmButtonsVisibility();
        } else {
            this.backButton = GuiUtils.createButton(
                    Component.translatable("gui.oneenoughitem.back"),
                    btn -> this.onClose(),
                    centerX - 40, this.height - 50, 80, 18
            );
            this.addRenderableWidget(this.backButton);
        }
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

        super.render(graphics, mouseX, mouseY, partialTick);

        GuiUtils.drawPanelBackground(graphics, panelX, panelY, panelW, panelH);

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
        if (this.isForMatch && hasControlDown()) {
            if (this.selectedTags.contains(tagId)) {
                this.selectedTags.remove(tagId);
            } else {
                this.selectedTags.add(tagId);
            }
            this.updateTagList();
            this.updateConfirmButtonsVisibility();
            return;
        }

        if (this.isForMatch) {
            this.parent.addMatchTag(tagId);
        } else {
            this.parent.setResultTag(tagId);
        }
        this.onClose();
    }

    private void confirmSelectedTags() {
        if (!this.isForMatch || this.selectedTags.isEmpty()) return;

        int added = 0;
        for (ResourceLocation tagId : new ArrayList<>(this.selectedTags)) {
            this.parent.addMatchTag(tagId);
            added++;
        }
        this.selectedTags.clear();
        this.updateTagList();
        this.updateConfirmButtonsVisibility();

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.oneenoughitem.multi_add_tags_result", added).withStyle(ChatFormatting.GREEN),
                    false
            );
        }
        this.onClose();
    }

    private void updateConfirmButtonsVisibility() {
        if (!this.isForMatch) return;
        boolean hasSelection = !this.selectedTags.isEmpty();
        if (this.confirmSelectionButton != null) {
            this.confirmSelectionButton.active = hasSelection;
        }
        if (this.clearSelectionButton != null) {
            this.clearSelectionButton.active = hasSelection;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}