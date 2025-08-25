package com.mafuyu404.oneenoughitem.client.gui;

import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.gui.components.ItemGridWidget;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemSelectionScreen extends Screen {
    private static final int ITEMS_PER_PAGE = 45; // 9x5
    private static final int GRID_WIDTH = 9;
    private static final int GRID_HEIGHT = 5;

    private final ReplacementEditorScreen parent;
    private final boolean isForMatch;

    private EditBox searchBox;
    private Button sortButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;

    private ItemGridWidget itemGrid;
    private List<Item> allItems;
    private List<Item> filteredItems;
    private int currentPage = 0;
    private SortMode sortMode = SortMode.NAME;

    public ItemSelectionScreen(ReplacementEditorScreen parent, boolean isForMatch) {
        super(Component.translatable("gui.oneenoughitem.item_selection.title"));
        this.parent = parent;
        this.isForMatch = isForMatch;
        this.initializeItems();
    }

    private void initializeItems() {
        this.allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());
        this.filteredItems = new ArrayList<>(this.allItems);
        this.sortItems();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        this.searchBox = new EditBox(this.font, centerX - 80, 15, 160, 18,
                Component.translatable("gui.oneenoughitem.search"));
        this.searchBox.setHint(Component.translatable("gui.oneenoughitem.search.hint"));
        this.addRenderableWidget(this.searchBox);

        this.sortButton = GuiUtils.createButton(
                Component.translatable("gui.oneenoughitem.sort." + this.sortMode.name().toLowerCase()),
                btn -> this.cycleSortMode(),
                centerX + 90, 15, 70, 18
        );
        this.addRenderableWidget(this.sortButton);

        int gridStartX = centerX - (GRID_WIDTH * 18) / 2;
        int gridStartY = 45;
        this.itemGrid = new ItemGridWidget(gridStartX, gridStartY, GRID_WIDTH, GRID_HEIGHT, this::selectItem);
        this.addRenderableWidget(this.itemGrid);

        int buttonY = gridStartY + GRID_HEIGHT * 18 + 10;
        this.prevPageButton = GuiUtils.createButton(Component.literal("<"),
                btn -> this.previousPage(),
                centerX - 80, buttonY, 25, 18);
        this.addRenderableWidget(this.prevPageButton);

        this.nextPageButton = GuiUtils.createButton(Component.literal(">"),
                btn -> this.nextPage(),
                centerX + 55, buttonY, 25, 18);
        this.addRenderableWidget(this.nextPageButton);

        this.backButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.back"),
                btn -> this.onClose(),
                centerX - 40, buttonY, 80, 18);
        this.addRenderableWidget(this.backButton);

        this.updateGrid();
        this.updateNavigationButtons();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.searchBox.getValue().length() != this.getLastSearchLength()) {
            this.filterItems();
            this.currentPage = 0;
            this.updateGrid();
            this.updateNavigationButtons();
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
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int totalPages = (this.filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        String pageInfo = (this.currentPage + 1) + " / " + Math.max(1, totalPages);
        int buttonY = 45 + GRID_HEIGHT * 18 + 10;
        graphics.drawCenteredString(this.font, pageInfo, this.width / 2, buttonY + 25, 0xFFFFFF);

        String itemCount = this.filteredItems.size() + " items";
        graphics.drawString(this.font, itemCount, 10, buttonY + 35, 0xFFFFFF);
    }

    private void cycleSortMode() {
        this.sortMode = SortMode.values()[(this.sortMode.ordinal() + 1) % SortMode.values().length];
        this.sortButton.setMessage(Component.translatable("gui.oneenoughitem.sort." + this.sortMode.name().toLowerCase()));
        this.sortItems();
        this.currentPage = 0;
        this.updateGrid();
        this.updateNavigationButtons();
    }

    private void sortItems() {
        switch (this.sortMode) {
            case NAME:
                this.filteredItems.sort(Comparator.comparing(item ->
                        item.getDescription().getString()));
                break;
            case MOD:
                this.filteredItems.sort(Comparator.comparing(item ->
                        BuiltInRegistries.ITEM.getKey(item).getNamespace()));
                break;
            case ID:
                this.filteredItems.sort(Comparator.comparing(item ->
                        BuiltInRegistries.ITEM.getKey(item).toString()));
                break;
        }
    }

    private void filterItems() {
        String search = this.searchBox.getValue().toLowerCase();
        if (search.isEmpty()) {
            this.filteredItems = new ArrayList<>(this.allItems);
        } else {
            this.filteredItems = this.allItems.stream()
                    .filter(item -> {
                        String itemName = item.getDescription().getString().toLowerCase();
                        String itemId = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
                        return itemName.contains(search) || itemId.contains(search);
                    })
                    .collect(Collectors.toList());
        }
        this.sortItems();
    }

    private void updateGrid() {
        List<ItemStack> pageItems = new ArrayList<>();
        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, this.filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int finalI = i;
            ItemStack itemStack = ReplacementControl.withSkipReplacement(() ->
                    new ItemStack(this.filteredItems.get(finalI)));
            pageItems.add(itemStack);
        }

        this.itemGrid.setItems(pageItems);
    }

    private void updateNavigationButtons() {
        int totalPages = (this.filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        this.prevPageButton.active = this.currentPage > 0;
        this.nextPageButton.active = this.currentPage < totalPages - 1;
    }

    private void previousPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.updateGrid();
            this.updateNavigationButtons();
        }
    }

    private void nextPage() {
        int totalPages = (this.filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (this.currentPage < totalPages - 1) {
            this.currentPage++;
            this.updateGrid();
            this.updateNavigationButtons();
        }
    }

    private void selectItem(ItemStack itemStack) {
        String itemId = Utils.getItemRegistryName(itemStack.getItem());
        if (itemId != null) {
            // 检查物品是否已被替换（优先检查运行时缓存）
            String runtimeReplacement = ReplacementCache.matchItem(itemId);
            String globalReplacement = GlobalReplacementCache.getItemReplacement(itemId);

            if (runtimeReplacement != null || globalReplacement != null) {
                // 显示错误消息
                if (this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.translatable("error.oneenoughitem.item_already_replaced").withStyle(ChatFormatting.RED),
                            false
                    );
                }
                return;
            }

            if (this.isForMatch) {
                // 对于匹配项，检查是否已经是其他规则的结果物品
                if (GlobalReplacementCache.isItemUsedAsResult(itemId)) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.translatable("error.oneenoughitem.item_used_as_result").withStyle(ChatFormatting.RED),
                                false
                        );
                    }
                    return;
                }
            } else {
                // 对于结果项，检查是否已经是其他规则的匹配项
                if (GlobalReplacementCache.isItemReplaced(itemId)) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.translatable("error.oneenoughitem.result_item_used_as_match").withStyle(ChatFormatting.RED),
                                false
                        );
                    }
                    return;
                }
            }
        }

        if (this.isForMatch) {
            this.parent.addMatchItem(itemStack.getItem());
        } else {
            this.parent.setResultItem(itemStack.getItem());
        }
        this.onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private enum SortMode {
        NAME, MOD, ID
    }
}