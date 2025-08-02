package com.mafuyu404.oneenoughitem.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModernFixWarningScreen extends Screen {
    private final Screen parentScreen;
    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_HEIGHT = 200;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    
    public ModernFixWarningScreen(Screen parentScreen) {
        super(Component.translatable("gui.oneenoughitem.modernfix_warning.title"));
        this.parentScreen = parentScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;

        Button confirmButton = Button.builder(
                Component.translatable("gui.oneenoughitem.modernfix_warning.confirm"),
                button -> {
                    this.minecraft.setScreen(parentScreen);
                }
        ).bounds(dialogX + DIALOG_WIDTH / 2 - BUTTON_WIDTH - 10, 
                dialogY + DIALOG_HEIGHT - 40, 
                BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();

        Button ignoreButton = Button.builder(
                Component.translatable("gui.oneenoughitem.modernfix_warning.ignore"),
                button -> this.minecraft.setScreen(parentScreen)
        ).bounds(dialogX + DIALOG_WIDTH / 2 + 10, 
                dialogY + DIALOG_HEIGHT - 40, 
                BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();
        
        this.addRenderableWidget(confirmButton);
        this.addRenderableWidget(ignoreButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);
        
        int dialogX = (this.width - DIALOG_WIDTH) / 2;
        int dialogY = (this.height - DIALOG_HEIGHT) / 2;

        guiGraphics.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, 0xFF2D2D30);
        guiGraphics.fill(dialogX + 1, dialogY + 1, dialogX + DIALOG_WIDTH - 1, dialogY + DIALOG_HEIGHT - 1, 0xFF3C3C3C);

        guiGraphics.fill(dialogX + 1, dialogY + 1, dialogX + DIALOG_WIDTH - 1, dialogY + 25, 0xFF4A90E2);

        Component title = Component.literal("OneEnoughItem").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, dialogX + (DIALOG_WIDTH - titleWidth) / 2, dialogY + 8, 0xFFFFFF);

        List<Component> warningLines = getWarningText();
        int textY = dialogY + 40;
        int lineHeight = this.font.lineHeight + 2;
        
        for (Component line : warningLines) {
            int textWidth = this.font.width(line);
            int textX = dialogX + (DIALOG_WIDTH - textWidth) / 2;
            guiGraphics.drawString(this.font, line, textX, textY, 0xFFFFFF);
            textY += lineHeight;
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private List<Component> getWarningText() {
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.translatable("gui.oneenoughitem.modernfix_warning.line1")
                .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.oneenoughitem.modernfix_warning.line2")
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("gui.oneenoughitem.modernfix_warning.line3")
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.oneenoughitem.modernfix_warning.line4")
                .withStyle(ChatFormatting.GRAY));
        
        return lines;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false; // 防止意外关闭
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }
}