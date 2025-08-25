package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class FileListWidget extends ObjectSelectionList<FileListWidget.FileEntry> {
    private final BiConsumer<Path, Integer> onFileSelect;
    private FileEntry selectedEntry = null;
    private final List<Button> actionButtons = new ArrayList<>();

    public FileListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, BiConsumer<Path, Integer> onFileSelect) {
        super(minecraft, width, height, y, y + height, itemHeight);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.onFileSelect = onFileSelect;
    }

    public void setFiles(List<PathUtils.FileInfo> files) {
        this.clearEntries();
        this.clearActionButtons();
        for (PathUtils.FileInfo file : files) {
            this.addEntry(new FileEntry(file));
        }
    }

    private void clearActionButtons() {
        this.actionButtons.clear();
        this.selectedEntry = null;
    }

    private void showActionButtons(FileEntry entry) {
        this.clearActionButtons();
        this.selectedEntry = entry;

        // 获取条目位置信息
        int entryIndex = this.children().indexOf(entry);
        int entryX = this.getRowLeft();
        int entryY = this.getRowTop(entryIndex);
        int entryWidth = this.getRowWidth();
        int entryHeight = this.itemHeight;

        // 计算文件内容的实际宽度
        int fileNameX = entryX + 16;
        String fileName = entry.fileInfo.displayName();
        String datapackName = "[" + entry.fileInfo.datapackName() + "]";
        String fullPath = entry.fileInfo.fullPath();

        int fileNameWidth = this.minecraft.font.width(fileName);
        int datapackWidth = this.minecraft.font.width(datapackName);
        int pathWidth = this.minecraft.font.width(fullPath);
        int pathX = fileNameX + datapackWidth + 5;
        int contentEndX = Math.max(fileNameX + fileNameWidth, pathX + pathWidth);

        // 按钮参数
        int buttonWidth = 30;
        int buttonHeight = 12;
        int spacing = 2;
        int buttonStartX = contentEndX + 8;

        // 确保按钮不会超出条目边界
        int maxButtonX = entryX + entryWidth - (buttonWidth * 3 + spacing * 2 + 5);
        if (buttonStartX > maxButtonX) {
            buttonStartX = maxButtonX;
        }

        int buttonY = entryY + (entryHeight - buttonHeight) / 2;

        Button addButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.add_button"),
                btn -> this.onFileSelect.accept(entry.fileInfo.filePath(), 0),
                buttonStartX, buttonY, buttonWidth, buttonHeight);

        Button editButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.modify_button"),
                btn -> this.onFileSelect.accept(entry.fileInfo.filePath(), 1),
                buttonStartX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight);

        Button deleteButton = GuiUtils.createButton(Component.translatable("gui.oneenoughitem.remove_button"),
                btn -> this.onFileSelect.accept(entry.fileInfo.filePath(), 2),
                buttonStartX + (buttonWidth + spacing) * 2, buttonY, buttonWidth, buttonHeight);

        this.actionButtons.add(addButton);
        this.actionButtons.add(editButton);
        this.actionButtons.add(deleteButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Button button : this.actionButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Button actionButton : this.actionButtons) {
            if (actionButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
        private final PathUtils.FileInfo fileInfo;

        public FileEntry(PathUtils.FileInfo fileInfo) {
            this.fileInfo = fileInfo;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean isMouseOver, float partialTick) {

            boolean isSelected = FileListWidget.this.selectedEntry == this;

            GuiUtils.drawFileEntryBackground(graphics, x, y, entryWidth, entryHeight, isMouseOver, isSelected);

            String fileName = this.fileInfo.displayName();
            String iconChar = fileName.isEmpty() ? "?" : String.valueOf(fileName.charAt(0)).toUpperCase();
            GuiUtils.drawFileIcon(graphics, x + 1, y + 4, iconChar);

            int fileNameX = x + 16;
            graphics.drawString(FileListWidget.this.minecraft.font, fileName, fileNameX, y + 2, 0xFFFFFF);

            String datapackName = "[" + this.fileInfo.datapackName() + "]";
            graphics.drawString(FileListWidget.this.minecraft.font, datapackName, fileNameX, y + 12, 0xAAAAAA);

            String fullPath = this.fileInfo.fullPath();
            int pathX = fileNameX + FileListWidget.this.minecraft.font.width(datapackName) + 5;
            graphics.drawString(FileListWidget.this.minecraft.font, fullPath, pathX, y + 12, 0x888888);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                FileListWidget.this.showActionButtons(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.fileInfo.displayName() + " in " + this.fileInfo.datapackName());
        }
    }
}