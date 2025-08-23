package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.cache.EditorCache;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.gui.components.ItemDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.components.ScrollablePanel;
import com.mafuyu404.oneenoughitem.client.gui.components.TagDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import com.mafuyu404.oneenoughitem.client.gui.util.ReplacementUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    private Button saveToJSONButton;
    private Button deduplicateRecipesButton;
    private Button objectDropdownButton;
    private boolean showObjectDropdown = false;
    private final List<Button> objectIndexButtons = new ArrayList<>();

    private Button addMatchItemButton;
    private Button addMatchTagButton;
    private Button clearMatchButton;
    private ScrollablePanel matchPanel;
    private final List<ItemDisplayWidget> matchItemWidgets;
    private final List<TagDisplayWidget> matchTagWidgets;

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
                ResourceLocation id = ResourceLocation.parse(itemId);
                Item item = BuiltInRegistries.ITEM.get(id);
                this.manager.addMatchItem(item);

                // 在跳过替换的情况下创建ItemStack
                ItemStack displayStack = ReplacementControl.withSkipReplacement(() -> new ItemStack(item));

                ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, displayStack,
                        button -> this.removeMatchItem(item), itemId, true);
                this.matchItemWidgets.add(widget);
            }

            for (String tagId : cache.matchTags()) {
                ResourceLocation id = ResourceLocation.parse(tagId);
                this.manager.addMatchTag(id);
                TagDisplayWidget widget = new TagDisplayWidget(0, 0, id,
                        button -> this.removeMatchTag(id));
                this.matchTagWidgets.add(widget);
            }

            if (cache.resultItem() != null) {
                ResourceLocation id = ResourceLocation.parse(cache.resultItem());
                Item item = BuiltInRegistries.ITEM.get(id);
                this.manager.setResultItem(item);
                this.resultItemWidget = new ItemDisplayWidget(0, 0, new ItemStack(item), null);
            }

            if (cache.resultTag() != null) {
                ResourceLocation id = ResourceLocation.parse(cache.resultTag());
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

        this.objectDropdownButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.object.select_element"),
                button -> this.toggleObjectDropdown(), 10, 10, 140, BUTTON_HEIGHT);
        this.updateObjectDropdownVisibility();

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

        this.saveToJSONButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.save_to_json"),
                button -> this.saveToJson(), centerX - 10, fileY + 25, 80, BUTTON_HEIGHT);
        this.addRenderableWidget(this.saveToJSONButton);


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

        int resultButtonSpacing = 10; // 按钮间距
        int totalResultButtonWidth = BUTTON_WIDTH + resultButtonSpacing + 50; // 选择物品按钮宽度 + 间距 + 清空按钮宽度
        int resultButtonStartX = rightPanelX + (PANEL_WIDTH - totalResultButtonWidth) / 2; // 居中计算起始位置

        this.selectResultItemButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.select_item"),
                button -> this.openItemSelection(false), resultButtonStartX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.selectResultItemButton);

        this.clearResultButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear"),
                button -> this.clearResultItem(), resultButtonStartX + BUTTON_WIDTH + resultButtonSpacing, panelY, 50, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearResultButton);

        this.matchPanel = new ScrollablePanel(leftPanelX + 5, panelY + 25, PANEL_WIDTH - 10, PANEL_HEIGHT - 30);
        this.addRenderableWidget(this.matchPanel);

        this.resultPanel = new ScrollablePanel(rightPanelX + 5, panelY + 25, PANEL_WIDTH - 10, PANEL_HEIGHT - 30);
        this.addRenderableWidget(this.resultPanel);

        this.rebuildPanels();
    }

    private void toggleObjectDropdown() {
        this.showObjectDropdown = !this.showObjectDropdown;
        this.updateObjectDropdown();
    }

    private void syncManagerDataToWidgets() {
        this.matchItemWidgets.clear();
        this.matchTagWidgets.clear();
        this.resultItemWidget = null;
        this.resultTagWidget = null;

        for (Item item : this.manager.getMatchItems()) {
            String originalItemId = Utils.getItemRegistryName(item);

            // 在跳过替换的情况下创建ItemStack，确保显示原始物品
            ItemStack displayStack = ReplacementControl.withSkipReplacement(() -> new ItemStack(item));

            // 匹配区域的物品显示跳过替换逻辑，显示原始物品
            ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, displayStack,
                    button -> {
                        this.removeMatchItemById(originalItemId);
                    }, originalItemId, true);
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

    private void removeMatchItemById(String itemId) {
        if (itemId != null) {
            ResourceLocation id = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);

            this.manager.removeMatchItem(item);

            this.matchItemWidgets.removeIf(widget -> itemId.equals(widget.getOriginalItemId()));

            this.rebuildPanels();
        }
    }


    private void updateObjectDropdown() {
        for (Button button : this.objectIndexButtons) {
            this.removeWidget(button);
        }
        this.objectIndexButtons.clear();

        if (this.showObjectDropdown && this.manager.getObjectSize() > 0) {
            int startX = this.objectDropdownButton.getX();
            int startY = this.objectDropdownButton.getY() + this.objectDropdownButton.getHeight() + 2;

            for (int i = 0; i < this.manager.getObjectSize(); i++) {
                final int index = i;
                String buttonText = String.valueOf(i + 1);
                if (this.manager.getCurrentObjectIndex() == i) {
                    buttonText += " ✓";
                }

                Button indexButton = GuiUtils.createObjectDropdownButton(Component.literal(buttonText),
                        button -> this.selectObjectIndex(index),
                        startX, startY + i * 22, 40, 20);
                this.objectIndexButtons.add(indexButton);
                this.addRenderableWidget(indexButton);
            }

            if (this.manager.getCurrentObjectIndex() >= 0) {
                Button deleteButton = GuiUtils.createObjectDropdownButton(Component.translatable("gui.oneenoughitem.object.delete"),
                        button -> this.deleteCurrentObjectElement(),
                        startX + 45, startY + this.manager.getCurrentObjectIndex() * 22, 50, 20);
                this.objectIndexButtons.add(deleteButton);
                this.addRenderableWidget(deleteButton);
            }
        }

        if (this.objectDropdownButton != null) {
            String buttonText = this.showObjectDropdown ?
                    Component.translatable("gui.oneenoughitem.object.element_up").getString() :
                    Component.translatable("gui.oneenoughitem.object.element_down").getString();
            if (this.manager.getCurrentObjectIndex() >= 0) {
                buttonText += " (" + (this.manager.getCurrentObjectIndex() + 1) + "/" + this.manager.getObjectSize() + ")";
            }
            this.objectDropdownButton.setMessage(Component.literal(buttonText));
        }
    }

    private void selectObjectIndex(int index) {
        this.manager.setCurrentObjectIndex(index);
        this.showObjectDropdown = false;
        this.updateObjectDropdown();
    }

    private void deleteCurrentObjectElement() {
        if (this.manager.getCurrentObjectIndex() >= 0) {
            this.manager.deleteObjectElement(this.manager.getCurrentObjectIndex());
            this.showObjectDropdown = false;
            this.updateObjectDropdown();
        }
    }

    private void updateObjectDropdownVisibility() {
        if (this.objectDropdownButton != null) {
            boolean shouldShow = this.manager.getCurrentObjectIndex() >= -1 && this.manager.getObjectSize() > 0;
            this.objectDropdownButton.visible = shouldShow;
            if (shouldShow) {
                this.addRenderableWidget(this.objectDropdownButton);
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

        if (this.showObjectDropdown && this.manager.getObjectSize() > 0) {
            int dropdownX = this.objectDropdownButton.getX();
            int dropdownY = this.objectDropdownButton.getY() + this.objectDropdownButton.getHeight() + 2;
            int dropdownWidth = 40;
            int dropdownHeight = this.manager.getObjectSize() * 22;

            GuiUtils.drawObjectDropdownBackground(graphics, dropdownX, dropdownY, dropdownWidth, dropdownHeight);

            if (this.manager.getCurrentObjectIndex() >= 0) {
                GuiUtils.drawObjectDropdownBackground(graphics, dropdownX + 45,
                        dropdownY + this.manager.getCurrentObjectIndex() * 22, 50, 20);
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
        switch (mode) {
            case 0 -> { // 添加模式
                this.manager.selectJsonFile(filePath, 0);
                this.syncManagerDataToWidgets();
            }
            case 1 -> { // 更改模式
                this.manager.selectJsonFile(filePath, 1);
                this.syncManagerDataToWidgets();
            }
            case 2 -> { // 删除模式
                this.manager.deleteFile(filePath);
            }
        }
    }

    private void removeMatchItem(Item item) {
        boolean removed = this.manager.removeMatchItem(item);
        if (removed) {
            this.syncManagerDataToWidgets();
        }
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
        this.updateObjectDropdownVisibility();
        this.updateObjectDropdown();
        EditorCache.clearCache();
    }

    private void openItemSelection(boolean isForMatch) {
        this.minecraft.setScreen(new ItemSelectionScreen(this, isForMatch));
    }

    private void openTagSelection(boolean isForMatch) {
        if (isForMatch) {
            this.minecraft.setScreen(new TagSelectionScreen(this, true));
        }
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
        String itemId = Utils.getItemRegistryName(item);
        if (itemId != null) {
            // 检查物品是否已被替换（优先检查运行时缓存）
            String runtimeReplacement = ReplacementCache.matchItem(itemId);
            String globalReplacement = GlobalReplacementCache.getItemReplacement(itemId);

            if (runtimeReplacement != null || globalReplacement != null) {
                this.showError(Component.translatable("error.oneenoughitem.item_already_replaced").withStyle(ChatFormatting.RED));
                return;
            }

            // 检查物品是否已经是其他规则的结果物品
            if (GlobalReplacementCache.isItemUsedAsResult(itemId)) {
                this.showError(Component.translatable("error.oneenoughitem.item_used_as_result").withStyle(ChatFormatting.RED));
                return;
            }
        }

        this.manager.addMatchItem(item);
        this.syncManagerDataToWidgets();
    }

    public void addMatchTag(ResourceLocation tagId) {
        if (this.manager.getMatchTags().contains(tagId)) {
            this.showWarn(Component.translatable("warning.oneenoughitem.tag_exists").withStyle(ChatFormatting.YELLOW));
            return;
        }

        ReplacementUtils.ReplacementInfo replacementInfo = ReplacementUtils.getTagReplacementInfo(tagId);
        if (replacementInfo.isReplaced()) {
            this.showError(Component.translatable("error.oneenoughitem.tag_already_replaced").withStyle(ChatFormatting.RED));
            return;
        }

        this.manager.addMatchTag(tagId);
        TagDisplayWidget widget = new TagDisplayWidget(0, 0, tagId,
                button -> this.removeMatchTag(tagId));
        this.matchTagWidgets.add(widget);
        this.rebuildPanels();
    }

    public void setResultItem(Item item) {
        String itemId = Utils.getItemRegistryName(item);
        if (itemId != null) {
            // 检查结果物品是否已被替换
            String runtimeReplacement = ReplacementCache.matchItem(itemId);
            String globalReplacement = GlobalReplacementCache.getItemReplacement(itemId);

            if (runtimeReplacement != null || globalReplacement != null) {
                this.showError(Component.translatable("error.oneenoughitem.result_item_already_replaced").withStyle(ChatFormatting.RED));
                return;
            }

            // 检查结果物品是否已经是其他规则的匹配项
            if (GlobalReplacementCache.isItemReplaced(itemId)) {
                this.showError(Component.translatable("error.oneenoughitem.result_item_used_as_match").withStyle(ChatFormatting.RED));
                return;
            }
        }

        this.manager.setResultItem(item);
        this.syncManagerDataToWidgets();
    }


    public void setResultTag(ResourceLocation tagId) {
        this.manager.setResultTag(tagId);
        this.resultTagWidget = new TagDisplayWidget(0, 0, tagId, null);
        this.resultItemWidget = null;
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
