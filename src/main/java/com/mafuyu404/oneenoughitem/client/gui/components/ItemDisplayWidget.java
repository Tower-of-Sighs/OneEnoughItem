package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.init.ReplacementControl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemDisplayWidget extends AbstractWidget {
    private final ItemStack itemStack;
    private final Button.OnPress removeAction;
    private final Font font = Minecraft.getInstance().font;
    private final String originalItemId;
    private final boolean skipReplacement;

    public ItemDisplayWidget(int x, int y, ItemStack itemStack, Button.OnPress removeAction) {
        this(x, y, itemStack, removeAction, null, false);
    }

    public ItemDisplayWidget(int x, int y, ItemStack itemStack, Button.OnPress removeAction, String originalItemId) {
        this(x, y, itemStack, removeAction, originalItemId, false);
    }

    public ItemDisplayWidget(int x, int y, ItemStack itemStack, Button.OnPress removeAction, String originalItemId, boolean skipReplacement) {
        super(x, y, 18, 18, Component.empty());
        this.itemStack = itemStack;
        this.removeAction = removeAction;
        this.originalItemId = originalItemId;
        this.skipReplacement = skipReplacement;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.getX(), this.getY(), this.getX() + 18, this.getY() + 18, 0xFF8B8B8B);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + 17, this.getY() + 17, 0xFF373737);

        if (!this.itemStack.isEmpty()) {
            if (skipReplacement) {
                ReplacementControl.withSkipReplacement(() -> {
                    graphics.renderItem(this.itemStack, this.getX() + 1, this.getY() + 1);
                    graphics.renderItemDecorations(Minecraft.getInstance().font, this.itemStack, this.getX() + 1, this.getY() + 1);
                });
            } else {
                graphics.renderItem(this.itemStack, this.getX() + 1, this.getY() + 1);
                graphics.renderItemDecorations(Minecraft.getInstance().font, this.itemStack, this.getX() + 1, this.getY() + 1);
            }
        }

        if (this.isHovered() && this.removeAction != null) {
            graphics.fill(this.getX() + 12, this.getY() - 2, this.getX() + 20, this.getY() + 6, 0xFFFF0000);
            graphics.drawString(font, "×", this.getX() + 14, this.getY(), 0xFFFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.isHovered() && !this.itemStack.isEmpty()) {
            List<Component> lines = new ArrayList<>();

            if (skipReplacement) {
                ReplacementControl.withSkipReplacement(() -> {
                    lines.add(this.itemStack.getHoverName());
                });
            } else {
                lines.add(this.itemStack.getHoverName());
            }

            String modId = BuiltInRegistries.ITEM.getKey(this.itemStack.getItem()).getNamespace();
            String modName = FabricLoader.getInstance().getModContainer(modId)
                    .map(ModContainer::getMetadata)
                    .map(ModMetadata::getName)
                    .orElse(modId);

            lines.add(Component.literal(modName).withStyle(ChatFormatting.BLUE));

            graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered() && this.removeAction != null) {
            if (mouseX >= this.getX() + 12 && mouseX <= this.getX() + 20 &&
                    mouseY >= this.getY() - 2 && mouseY <= this.getY() + 6) {

                this.removeAction.onPress(null);
                return true;
            }
        }
        return false;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public String getOriginalItemId() {
        return this.originalItemId;
    }

    public void setPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.itemStack.getHoverName());
    }
}