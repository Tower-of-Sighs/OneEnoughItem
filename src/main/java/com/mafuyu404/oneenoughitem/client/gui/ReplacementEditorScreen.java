package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.cache.EditorCache;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.gui.components.ItemDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.components.ScrollablePanel;
import com.mafuyu404.oneenoughitem.client.gui.components.TagDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.editor.FileActions;
import com.mafuyu404.oneenoughitem.client.gui.editor.ItemsController;
import com.mafuyu404.oneenoughitem.client.gui.editor.ObjectDropdownController;
import com.mafuyu404.oneenoughitem.client.gui.editor.PanelsLayoutHelper;
import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import com.mafuyu404.oneenoughitem.client.gui.util.*;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mafuyu404.oneenoughitem.web.WebEditorServer;
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
    private static final float EDITOR_SCALE_X = 1.24f;
    private static final float EDITOR_SCALE_Y = 2.255f;
    private static final int EDITOR_SHIFT_X = 5;
    private static final int EDITOR_SHIFT_Y = 0;
    private static final int EDITOR_INNER_MARGIN_X = 13;
    private static final int EDITOR_HEADER_GAP = 13;
    private static final int EDITOR_BOTTOM_MARGIN = 14;
    private final ReplacementEditorManager manager;

    private EditBox datapackNameBox;
    private EditBox fileNameBox;
    private Button createFileButton;
    private Button selectFileButton;
    private Button reloadButton;
    private Button clearAllButton;
    private Button saveToJSONButton;
    private Button openWebEditorButton;

    private Button objectDropdownButton;
    private boolean showObjectDropdown = false;
    private final List<Button> objectIndexButtons = new ArrayList<>();
    private ScrollablePanel objectDropdownPanel;

    private Button addMatchItemButton;
    private Button addMatchTagButton;
    private Button clearMatchButton;
    private ScrollablePanel matchPanel;
    private final List<ItemDisplayWidget> matchItemWidgets;
    private final List<TagDisplayWidget> matchTagWidgets;

    private Button selectResultItemButton;
    private Button clearResultButton;
    private ScrollablePanel resultPanel;
    private ItemDisplayWidget resultItemWidget;
    private TagDisplayWidget resultTagWidget;

    private final PanelsLayoutHelper panelsHelper = new PanelsLayoutHelper();
    private final FileActions fileActions;
    private final ItemsController itemsController;

    public ReplacementEditorScreen() {
        super(Component.translatable("gui.oneenoughitem.replacement_editor.title"));
        this.manager = new ReplacementEditorManager();
        this.matchItemWidgets = new ArrayList<>();
        this.matchTagWidgets = new ArrayList<>();
        this.fileActions = new FileActions(this.manager);
        this.itemsController = new ItemsController(this.manager, this::syncManagerDataToWidgets);

        this.loadFromCache();
    }

    private void loadFromCache() {
        EditorCache.CacheData cache = EditorCache.loadCache();
        if (cache != null) {
            for (String itemId : cache.matchItems()) {
                ResourceLocation id = new ResourceLocation(itemId);
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    this.manager.addMatchItem(item);

                    // 在跳过替换的情况下创建ItemStack
                    ItemStack displayStack = ReplacementControl.withSkipReplacement(() -> new ItemStack(item));

                    ItemDisplayWidget widget = new ItemDisplayWidget(0, 0, displayStack,
                            button -> this.removeMatchItem(item), itemId, true);
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
                Item item = BuiltInRegistries.ITEM.get(id);
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

        int webBtnX = (centerX - 10) + 80 + 8;
        this.openWebEditorButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.web_rules_injector"),
                button -> this.openWebEditor(), webBtnX, fileY + 25, 90, BUTTON_HEIGHT);
        this.addRenderableWidget(this.openWebEditorButton);

        int panelY = fileY + 55;
        int leftPanelX = centerX - PANEL_WIDTH - MARGIN;
        int rightPanelX = centerX + MARGIN;
        int offsetX = 10;

        this.addMatchItemButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.add_item"),
                button -> this.openItemSelection(true), leftPanelX + 5 + offsetX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.addMatchItemButton);

        this.addMatchTagButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.add_tag"),
                button -> this.openTagSelection(true), leftPanelX + BUTTON_WIDTH + 8 + offsetX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.addMatchTagButton);

        this.clearMatchButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear"),
                button -> this.clearMatchItems(), leftPanelX + BUTTON_WIDTH * 2 + 11 + offsetX, panelY, 50, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearMatchButton);

        int resultButtonSpacing = 10;
        int totalResultButtonWidth = BUTTON_WIDTH + resultButtonSpacing + 50;
        int resultButtonStartX = rightPanelX + (PANEL_WIDTH - totalResultButtonWidth) / 2 + offsetX;

        this.selectResultItemButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.select_item"),
                button -> this.openItemSelection(false), resultButtonStartX, panelY, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.addRenderableWidget(this.selectResultItemButton);

        this.clearResultButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.clear"),
                button -> this.clearResultItem(), resultButtonStartX + BUTTON_WIDTH + resultButtonSpacing, panelY, 50, BUTTON_HEIGHT);
        this.addRenderableWidget(this.clearResultButton);

        int editorW = Math.round(PANEL_WIDTH * EDITOR_SCALE_X);
        int editorH = Math.round(PANEL_HEIGHT * EDITOR_SCALE_Y);
        int editorY = panelY + EDITOR_SHIFT_Y;
        int leftEditorX = leftPanelX + EDITOR_SHIFT_X;

        int marginX = Math.round(EDITOR_INNER_MARGIN_X * EDITOR_SCALE_X);
        int headerGap = Math.round(EDITOR_HEADER_GAP * EDITOR_SCALE_Y);
        int bottomMargin = Math.round(EDITOR_BOTTOM_MARGIN * EDITOR_SCALE_Y);

        this.matchPanel = new ScrollablePanel(
                leftEditorX + marginX,
                editorY + headerGap,
                editorW - marginX * 2,
                editorH - headerGap - bottomMargin
        );
        this.addRenderableWidget(this.matchPanel);

        this.resultPanel = new ScrollablePanel(rightPanelX + 12, panelY + 32, PANEL_WIDTH - 10, PANEL_HEIGHT - 30);
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
                    }, originalItemId, true); // 设置skipReplacement为true
            this.matchItemWidgets.add(widget);
        }
        for (ResourceLocation tagId : this.manager.getMatchTags()) {
            TagDisplayWidget widget = new TagDisplayWidget(0, 0, tagId,
                    button -> this.removeMatchTag(tagId));
            this.matchTagWidgets.add(widget);
        }

        if (this.manager.getResultItem() != null) {
            // 结果区域正常显示（会应用替换）
            this.resultItemWidget = new ItemDisplayWidget(0, 0, new ItemStack(this.manager.getResultItem()), null);
        }

        if (this.manager.getResultTag() != null) {
            this.resultTagWidget = new TagDisplayWidget(0, 0, this.manager.getResultTag(), null);
        }

        this.rebuildPanels();
    }

    private void removeMatchItemById(String itemId) {
        if (itemId != null) {
            ResourceLocation id = new ResourceLocation(itemId);
            Item item = BuiltInRegistries.ITEM.get(id);

            if (item != null) {
                this.manager.removeMatchItem(item);

                this.matchItemWidgets.removeIf(widget -> itemId.equals(widget.getOriginalItemId()));

                this.rebuildPanels();
            }
        }
    }

    private void updateObjectDropdown() {
        for (Button b : this.objectIndexButtons) {
            this.removeWidget(b);
        }
        this.objectIndexButtons.clear();

        this.objectDropdownPanel = ObjectDropdownController.rebuildDropdownPanel(
                this.objectDropdownButton,
                this.showObjectDropdown,
                this.manager,
                this.objectIndexButtons,
                this::selectObjectIndex,
                this::deleteCurrentObjectElement
        );

        if (this.objectDropdownButton != null) {
            this.objectDropdownButton.setMessage(
                    ObjectDropdownController.buildDropdownButtonText(this.showObjectDropdown, this.manager)
            );
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
        int centerX = this.width / 2;
        int topY = 15;
        int fileY = topY + 25;
        int panelY = fileY + 55;
        int leftPanelX = centerX - PANEL_WIDTH - MARGIN;
        int rightPanelX = centerX + MARGIN;

        int editorW = Math.round(PANEL_WIDTH * EDITOR_SCALE_X);
        int editorH = Math.round(PANEL_HEIGHT * EDITOR_SCALE_Y);
        int editorY = panelY + EDITOR_SHIFT_Y;
        int leftEditorX = leftPanelX + EDITOR_SHIFT_X;
        int rightEditorX = rightPanelX + EDITOR_SHIFT_X;

        GuiUtils.drawEditorPanel(graphics, leftEditorX, editorY, editorW, editorH);
        GuiUtils.drawEditorPanel(graphics, rightEditorX, editorY, editorW, editorH);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(Minecraft.getInstance().font, this.title,
                centerX, 20, 0xFFFFFF);

        Component fileName = this.manager.getCurrentFileName().isEmpty()
                ? Component.translatable("gui.oneenoughitem.current_file.none").withStyle(ChatFormatting.GRAY)
                : Component.literal(this.manager.getCurrentFileName()).withStyle(ChatFormatting.AQUA);

        Component fileInfo = Component.translatable("gui.oneenoughitem.current_file", fileName);
        graphics.drawCenteredString(Minecraft.getInstance().font, fileInfo, centerX, 8, 0xAAAAAA);

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

        if (this.showObjectDropdown && this.objectDropdownPanel != null) {
            this.objectDropdownPanel.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void createFile() {
        String datapackName = this.datapackNameBox.getValue().trim();
        String fileName = this.fileNameBox.getValue().trim();

        if (fileName.isEmpty()) {
            this.showError(Component.translatable("error.oneenoughitem.file_name_empty").withStyle(ChatFormatting.RED));
            return;
        }

        String newName = this.fileActions.createFile(datapackName, fileName);
        this.fileNameBox.setValue(newName);
    }

    private void saveToJson() {
        this.fileActions.saveToJson();
    }

    private void openWebEditor() {
        String message = WebEditorServer.openInBrowser();
        if (message != null) {
            this.showMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
        }
    }

    private void selectFile() {
        this.fileActions.selectFile(this.minecraft, this);
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
        boolean removed = this.itemsController.removeMatchItem(item);
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
        this.itemsController.clearMatchItems();
        this.rebuildPanels();
    }

    private void clearResultItem() {
        this.resultItemWidget = null;
        this.resultTagWidget = null;
        this.itemsController.clearResultItem();
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

        this.itemsController.addMatchItem(item);
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

        this.itemsController.addMatchTag(tagId);
        this.syncManagerDataToWidgets();
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

        this.itemsController.setResultItem(item);
        this.syncManagerDataToWidgets();
    }

    public void setResultTag(ResourceLocation tagId) {
        this.itemsController.setResultTag(tagId);
        this.resultTagWidget = new TagDisplayWidget(0, 0, tagId, null);
        this.resultItemWidget = null;
        this.rebuildPanels();
    }

    private void removeMatchTag(ResourceLocation tagId) {
        this.itemsController.removeMatchTag(tagId);
        this.matchTagWidgets.removeIf(widget -> widget.getTagId().equals(tagId));
        this.rebuildPanels();
    }

    protected void rebuildPanels() {
        this.panelsHelper.rebuildPanels(
                this.matchPanel,
                this.resultPanel,
                this.matchItemWidgets,
                this.matchTagWidgets,
                this.resultItemWidget,
                this.resultTagWidget
        );
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
        return this.fileActions.scanReplacementFiles();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先交给下拉菜单处理，保证其优先级
        if (this.showObjectDropdown && this.objectDropdownPanel != null) {
            if (this.objectDropdownPanel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.showObjectDropdown && this.objectDropdownPanel != null) {
            if (this.objectDropdownPanel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.showObjectDropdown && this.objectDropdownPanel != null) {
            if (this.objectDropdownPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.showObjectDropdown && this.objectDropdownPanel != null) {
            if (this.objectDropdownPanel.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}