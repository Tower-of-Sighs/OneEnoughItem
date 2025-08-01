package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.cache.EditorCache;
import com.mafuyu404.oneenoughitem.client.gui.components.ItemDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.components.ScrollablePanel;
import com.mafuyu404.oneenoughitem.client.gui.components.TagDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ReplacementEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 140;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 18;
    private static final int MARGIN = 8;

    private final ReplacementEditorManager manager;

    private EditBox datapackNameBox;
    private EditBox fileNameBox;
    private Button createFileButton;
    private Button selectFileButton;
    private Button reloadButton;
    private Button clearAllButton;

    private Button arrayDropdownButton;
    private boolean showArrayDropdown = false;
    private List<Button> arrayIndexButtons = new ArrayList<>();

    private Button addMatchItemButton;
    private Button addMatchTagButton;
    private Button clearMatchButton;
    private ScrollablePanel matchPanel;
    private List<ItemDisplayWidget> matchItemWidgets;
    private List<TagDisplayWidget> matchTagWidgets;

    private Button selectResultItemButton;
    private Button selectResultTagButton;
    private Button clearResultButton;
    private ScrollablePanel resultPanel;
    private ItemDisplayWidget resultItemWidget;
    private TagDisplayWidget resultTagWidget;

    public ReplacementEditorScreen() {
        super(Component.translatable("gui.oneenoughitem.replacement_editor.title"));
        this.manager = new ReplacementEditorManager();
        this.matchItemWidgets = new ArrayList<>();
        this.matchTagWidgets = new ArrayList<>();
        this.loadFromCache();
    }

    private void loadFromCache() {
        EditorCache.CacheData cache = EditorCache.loadCache();
        if (cache != null) {
            for (String itemId : cache.matchItems()) {
                ResourceLocation id = new ResourceLocation(itemId);
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    this.manager.addMatchItem(item);
                    ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, new ItemStack(item),
                            button -> this.removeMatchItem(item));
                    this.matchItemWidgets.add(widget);
                }
            }

            for (String tagId : cache.matchTags()) {
                ResourceLocation id = new ResourceLocation(tagId);
                this.manager.addMatchTag(id);
                TagDisplayWidget widget = new TagDisplayWidget(0, 0, id,
                        button -> this.removeMatchTag(id));
                this.matchTagWidgets.add(widget);
            }

            if (cache.resultItem() != null) {
                ResourceLocation id = new ResourceLocation(cache.resultItem());
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    this.manager.setResultItem(item);
                    this.resultItemWidget = new ItemDisplayWidget(0, 0, new ItemStack(item), null);
                }
            }

            if (cache.resultTag() != null) {
                ResourceLocation id = new ResourceLocation(cache.resultTag());
                this.manager.setResultTag(id);
                this.resultTagWidget = new TagDisplayWidget(0, 0, id, null);
            }

            if (cache.fileName() != null) {
                this.manager.setCurrentFileName(cache.fileName());
            }
        }
    }

    private void saveToCache() {
        EditorCache.saveCache(
                this.manager.getMatchItems(),
                this.manager.getMatchTags(),
                this.manager.getResultItem(),
                this.manager.getResultTag(),
                this.manager.getCurrentFileName()
        );
        this.showMessage(Component.translatable("message.oneenoughitem.cache_saved").withStyle(ChatFormatting.GREEN));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int topY = 15;

        this.manager.setUiUpdateCallback(this::syncManagerDataToWidgets);

        this.arrayDropdownButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.array.select_element"),
                button -> this.toggleArrayDropdown(), 10, 10, 140, BUTTON_HEIGHT);
        this.updateArrayDropdownVisibility();

        int fileY = topY + 25;
        this.datapackNameBox = new EditBox(Minecraft.getInstance().font, centerX - 140, fileY, 80, 18,
                Component.translatable("gui.oneenoughitem.datapack_name"));
        this.datapackNameBox.setHint(Component.translatable("gui.oneenoughitem.datapack_name.hint"));
        this.addRenderableWidget(this.datapackNameBox);

        this.fileNameBox = new EditBox(Minecraft.getInstance().font, centerX - 55, fileY, 80, 18,
                Component.translatable("gui.oneenoughitem.file_name"));
        this.fileNameBox.setHint(Component.translatable("gui.oneenoughitem.file_name.hint"));
        this.fileNameBox.setValue(this.manager.getCurrentFileName());
        this.addRenderableWidget(this.fileNameBox);

        this.createFileButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.create_file"),
                button -> this.createFile(), centerX + 30, fileY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.createFileButton);

        this.selectFileButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.select_file"),
                button -> this.selectFile(), centerX + 105, fileY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.selectFileButton);

        this.reloadButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.reload"),
                button -> this.reloadDatapacks(), centerX - 140, fileY + 25, 60, BUTTON_HEIGHT);
        this.addRenderableWidget(this.reloadButton);

        this.clearAllButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear_all"),
                button -> this.clearAll(), centerX - 75, fileY + 25, 60, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearAllButton);

        Button saveToJsonButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.save_to_json"),
                button -> this.saveToJson(), centerX - 10, fileY + 25, 80, BUTTON_HEIGHT);
        this.addRenderableWidget(saveToJsonButton);

        int panelY = fileY + 55;
        int leftPanelX = centerX - PANEL_WIDTH - MARGIN;
        int rightPanelX = centerX + MARGIN;

        this.addMatchItemButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.add_item"),
                button -> this.openItemSelection(true), leftPanelX + 5, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.addMatchItemButton);

        this.addMatchTagButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.add_tag"),
                button -> this.openTagSelection(true), leftPanelX + BUTTON_WIDTH + 8, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.addMatchTagButton);

        this.clearMatchButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear"),
                button -> this.clearMatchItems(), leftPanelX + BUTTON_WIDTH * 2 + 11, panelY, 50, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearMatchButton);

        this.selectResultItemButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.select_item"),
                button -> this.openItemSelection(false), rightPanelX + 5, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.selectResultItemButton);

        this.selectResultTagButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.select_tag"),
                button -> this.openTagSelection(false), rightPanelX + BUTTON_WIDTH + 8, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.selectResultTagButton);

        this.clearResultButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear"),
                button -> this.clearResultItem(), rightPanelX + BUTTON_WIDTH * 2 + 11, panelY, 50, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearResultButton);

        this.matchPanel = new ScrollablePanel(leftPanelX + 5, panelY + 25, PANEL_WIDTH - 10, PANEL_HEIGHT - 30);
        this.addRenderableWidget(this.matchPanel);

        this.resultPanel = new ScrollablePanel(rightPanelX + 5, panelY + 25, PANEL_WIDTH - 10, PANEL_HEIGHT - 30);
        this.addRenderableWidget(this.resultPanel);

        this.rebuildPanels();
    }

    private void toggleArrayDropdown() {
        this.showArrayDropdown = !this.showArrayDropdown;
        this.updateArrayDropdown();
    }

    private void syncManagerDataToWidgets() {
        this.matchItemWidgets.clear();
        this.matchTagWidgets.clear();
        this.resultItemWidget = null;
        this.resultTagWidget = null;

        for (Item item : this.manager.getMatchItems()) {
            ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, new ItemStack(item),
                    button -> this.removeMatchItem(item));
            this.matchItemWidgets.add(widget);
        }

        for (ResourceLocation tagId : this.manager.getMatchTags()) {
            TagDisplayWidget widget = new TagDisplayWidget(0, 0, tagId,
                    button -> this.removeMatchTag(tagId));
            this.matchTagWidgets.add(widget);
        }

        if (this.manager.getResultItem() != null) {
            this.resultItemWidget = new ItemDisplayWidget(0, 0, new ItemStack(this.manager.getResultItem()), null);
        }

        if (this.manager.getResultTag() != null) {
            this.resultTagWidget = new TagDisplayWidget(0, 0, this.manager.getResultTag(), null);
        }

        this.rebuildPanels();
    }

    private void updateArrayDropdown() {
        for (Button button : this.arrayIndexButtons) {
            this.removeWidget(button);
        }
        this.arrayIndexButtons.clear();

        if (this.showArrayDropdown && this.manager.getArraySize() > 0) {
            int startX = this.arrayDropdownButton.getX();
            int startY = this.arrayDropdownButton.getY() + this.arrayDropdownButton.getHeight() + 2;

            for (int i = 0; i < this.manager.getArraySize(); i++) {
                final int index = i;
                String buttonText = String.valueOf(i + 1);
                if (this.manager.getCurrentArrayIndex() == i) {
                    buttonText += " ✓";
                }

                Button indexButton = GuiUtils.createArrayDropdownButton(Component.literal(buttonText),
                        button -> this.selectArrayIndex(index),
                        startX, startY + i * 22, 40, 20);
                this.arrayIndexButtons.add(indexButton);
                this.addRenderableWidget(indexButton);
            }

            if (this.manager.getCurrentArrayIndex() >= 0) {
                Button deleteButton = GuiUtils.createArrayDropdownButton(Component.translatable("gui.oneenoughitem.array.delete"),
                        button -> this.deleteCurrentArrayElement(),
                        startX + 45, startY + this.manager.getCurrentArrayIndex() * 22, 50, 20);
                this.arrayIndexButtons.add(deleteButton);
                this.addRenderableWidget(deleteButton);
            }
        }

        if (this.arrayDropdownButton != null) {
            String buttonText = this.showArrayDropdown ?
                    Component.translatable("gui.oneenoughitem.array.element_up").getString() :
                    Component.translatable("gui.oneenoughitem.array.element_down").getString();
            if (this.manager.getCurrentArrayIndex() >= 0) {
                buttonText += " (" + (this.manager.getCurrentArrayIndex() + 1) + "/" + this.manager.getArraySize() + ")";
            }
            this.arrayDropdownButton.setMessage(Component.literal(buttonText));
        }
    }

    private void selectArrayIndex(int index) {
        this.manager.setCurrentArrayIndex(index);
        this.showArrayDropdown = false;
        this.updateArrayDropdown();
    }

    private void deleteCurrentArrayElement() {
        if (this.manager.getCurrentArrayIndex() >= 0) {
            this.manager.deleteArrayElement(this.manager.getCurrentArrayIndex());
            this.showArrayDropdown = false;
            this.updateArrayDropdown();
        }
    }

    private void updateArrayDropdownVisibility() {
        if (this.arrayDropdownButton != null) {
            boolean shouldShow = this.manager.getCurrentArrayIndex() >= -1 && this.manager.getArraySize() > 0;
            this.arrayDropdownButton.visible = shouldShow;
            if (shouldShow) {
                this.addRenderableWidget(this.arrayDropdownButton);
            }
        }
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            this.saveToCache();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int topY = 15;
        int fileY = topY + 25;
        int panelY = fileY + 55;
        int leftPanelX = centerX - PANEL_WIDTH - MARGIN;
        int rightPanelX = centerX + MARGIN;

        graphics.drawCenteredString(Minecraft.getInstance().font, this.title,
                centerX, 20, 0xFFFFFF);

        Component fileName = this.manager.getCurrentFileName().isEmpty()
                ? Component.translatable("gui.oneenoughitem.current_file.none").withStyle(ChatFormatting.GRAY)
                : Component.literal(this.manager.getCurrentFileName()).withStyle(ChatFormatting.AQUA);

        Component fileInfo = Component.translatable("gui.oneenoughitem.current_file", fileName);
        graphics.drawCenteredString(Minecraft.getInstance().font, fileInfo, centerX, 8, 0xAAAAAA);

        if (this.showArrayDropdown && this.manager.getArraySize() > 0) {
            int dropdownX = this.arrayDropdownButton.getX();
            int dropdownY = this.arrayDropdownButton.getY() + this.arrayDropdownButton.getHeight() + 2;
            int dropdownWidth = 40;
            int dropdownHeight = this.manager.getArraySize() * 22;

            GuiUtils.drawArrayDropdownBackground(graphics, dropdownX, dropdownY, dropdownWidth, dropdownHeight);

            if (this.manager.getCurrentArrayIndex() >= 0) {
                GuiUtils.drawArrayDropdownBackground(graphics, dropdownX + 45,
                        dropdownY + this.manager.getCurrentArrayIndex() * 22, 50, 20);
            }
        }

        GuiUtils.drawPanelBackground(graphics, leftPanelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        GuiUtils.drawPanelBackground(graphics, rightPanelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.oneenoughitem.match_items"),
                leftPanelX + PANEL_WIDTH / 2, panelY - 12, 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("gui.oneenoughitem.result_item"),
                rightPanelX + PANEL_WIDTH / 2, panelY - 12, 0xFFFFFF);

        String matchCount = Component.translatable("gui.oneenoughitem.match_summary",
                this.matchItemWidgets.size(), this.matchTagWidgets.size()).getString();
        graphics.drawString(Minecraft.getInstance().font, matchCount, leftPanelX, panelY + PANEL_HEIGHT + 8, 0xAAAAAA);

        Component resultText = this.resultItemWidget != null
                ? Component.translatable("gui.oneenoughitem.item_selected")
                : this.resultTagWidget != null
                ? Component.translatable("gui.oneenoughitem.tag_selected")
                : Component.translatable("gui.oneenoughitem.no_result");
        graphics.drawString(Minecraft.getInstance().font, resultText, rightPanelX, panelY + PANEL_HEIGHT + 8, 0xAAAAAA);

        graphics.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.oneenoughitem.save_to_cache"),
                centerX, panelY + PANEL_HEIGHT + 25, 0xFFFF00);
    }

    private void createFile() {
        String datapackName = this.datapackNameBox.getValue().trim();
        String fileName = this.fileNameBox.getValue().trim();

        if (fileName.isEmpty()) {
            this.showError(Component.translatable("error.oneenoughitem.file_name_empty").withStyle(ChatFormatting.RED));
            return;
        }

        this.manager.createReplacementFile(datapackName, fileName);
        this.fileNameBox.setValue(this.manager.getCurrentFileName());
    }
    private void saveToJson() {
        this.manager.saveReplacement();
    }

    private void selectFile() {
        this.minecraft.setScreen(new FileSelectionScreen(this));
    }

    public void onFileSelected(Path filePath, int mode) {
        this.manager.selectJsonFile(filePath, mode);
        this.fileNameBox.setValue(this.manager.getCurrentFileName());

        String modeKey = switch (mode) {
            case 0 -> "Add";
            case 1 -> "Modify";
            case 2 -> "Remove";
            default -> "Unknown";
        };

        this.showMessage(Component.translatable("message.oneenoughitem.file_selected",
                filePath.getFileName().toString(),
                Component.translatable(modeKey)).withStyle(ChatFormatting.GREEN));

        this.updateArrayDropdownVisibility();
        this.updateArrayDropdown();
        this.rebuildPanels();
    }


    private void reloadDatapacks() {
        this.manager.reloadDatapacks();
    }

    private void clearAll() {
        this.manager.clearAll();
        this.matchItemWidgets.clear();
        this.matchTagWidgets.clear();
        this.resultItemWidget = null;
        this.resultTagWidget = null;
        this.fileNameBox.setValue("");
        this.rebuildPanels();
        this.updateArrayDropdownVisibility();
        this.updateArrayDropdown();
        EditorCache.clearCache();
    }

    private void openItemSelection(boolean isForMatch) {
        this.minecraft.setScreen(new ItemSelectionScreen(this, isForMatch));
    }

    private void openTagSelection(boolean isForMatch) {
        this.minecraft.setScreen(new TagSelectionScreen(this, isForMatch));
    }

    private void clearMatchItems() {
        this.matchItemWidgets.clear();
        this.matchTagWidgets.clear();
        this.manager.clearMatchItems();
        this.rebuildPanels();
    }

    private void clearResultItem() {
        this.resultItemWidget = null;
        this.resultTagWidget = null;
        this.manager.clearResultItem();
        this.rebuildPanels();
    }

    public void addMatchItem(Item item) {
        if (this.manager.getMatchItems().contains(item)) {
            this.showWarn(Component.translatable("warning.oneenoughitem.item_exists").withStyle(ChatFormatting.YELLOW));
            return;
        }

        this.manager.addMatchItem(item);
        ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, new ItemStack(item),
                button -> this.removeMatchItem(item));
        this.matchItemWidgets.add(widget);
        this.rebuildPanels();
    }

    public void addMatchTag(ResourceLocation tagId) {
        if (this.manager.getMatchTags().contains(tagId)) {
            this.showWarn(Component.translatable("warning.oneenoughitem.tag_exists").withStyle(ChatFormatting.YELLOW));
            return;
        }

        this.manager.addMatchTag(tagId);
        TagDisplayWidget widget = new TagDisplayWidget(0, 0, tagId,
                button -> this.removeMatchTag(tagId));
        this.matchTagWidgets.add(widget);
        this.rebuildPanels();
    }

    public void setResultItem(Item item) {
        this.manager.setResultItem(item);
        this.resultItemWidget = new ItemDisplayWidget(0, 0, new ItemStack(item), null);
        this.resultTagWidget = null;
        this.rebuildPanels();
    }

    public void setResultTag(ResourceLocation tagId) {
        this.manager.setResultTag(tagId);
        this.resultTagWidget = new TagDisplayWidget(0, 0, tagId, null);
        this.resultItemWidget = null;
        this.rebuildPanels();
    }

    private void removeMatchItem(Item item) {
        this.manager.removeMatchItem(item);
        this.matchItemWidgets.removeIf(widget -> widget.getItem().getItem() == item);
        this.rebuildPanels();
    }

    private void removeMatchTag(ResourceLocation tagId) {
        this.manager.removeMatchTag(tagId);
        this.matchTagWidgets.removeIf(widget -> widget.getTagId().equals(tagId));
        this.rebuildPanels();
    }

    protected void rebuildPanels() {
        this.matchPanel.clearWidgets();
        this.resultPanel.clearWidgets();

        int itemsPerRow = 10;
        int itemSize = 18;
        int spacing = 2;
        int index = 0;

        for (ItemDisplayWidget widget : this.matchItemWidgets) {
            int row = index / itemsPerRow;
            int col = index % itemsPerRow;
            int x = this.matchPanel.getX() + col * (itemSize + spacing);
            int y = this.matchPanel.getY() + row * (itemSize + spacing);
            widget.setPosition(x, y);
            this.matchPanel.addWidget(widget);
            index++;
        }

        int tagStartY = ((this.matchItemWidgets.size() + itemsPerRow - 1) / itemsPerRow) * (itemSize + spacing) + 10;
        index = 0;
        for (TagDisplayWidget widget : this.matchTagWidgets) {
            int row = index / 3;
            int col = index % 3;
            int x = this.matchPanel.getX() + col * 70;
            int y = this.matchPanel.getY() + tagStartY + row * 22;
            widget.setPosition(x, y);
            this.matchPanel.addWidget(widget);
            index++;
        }

        if (this.resultItemWidget != null) {
            this.resultItemWidget.setPosition(this.resultPanel.getX() + this.resultPanel.getWidth() / 2 - 9,
                    this.resultPanel.getY() + 20);
            this.resultPanel.addWidget(this.resultItemWidget);
        }
        if (this.resultTagWidget != null) {
            this.resultTagWidget.setPosition(this.resultPanel.getX() + this.resultPanel.getWidth() / 2 - 35,
                    this.resultPanel.getY() + 20);
            this.resultPanel.addWidget(this.resultTagWidget);
        }

        this.matchPanel.updateWidgetPositions();
        this.resultPanel.updateWidgetPositions();
    }

    private void showError(Component message) {
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(message, false);
        }
    }

    private void showWarn(Component message) {
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(message, false);
        }
    }

    private void showMessage(Component message) {
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(message, false);
        }
    }

    public List<Path> scanReplacementFiles() {
        List<Path> jsonFiles = new ArrayList<>();
        try {
            Path replacementsPath = PathUtils.getReplacementsPath();
            if (Files.exists(replacementsPath)) {
                try (Stream<Path> paths = Files.walk(replacementsPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                            .forEach(jsonFiles::add);
                }
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to scan replacement files", e);
        }
        return jsonFiles;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}