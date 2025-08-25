package com.mafuyu404.oneenoughitem.client.gui.components;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemDisplayWidget extends AbstractWidget {
    private final ItemStack itemStack;
    private final Button.OnPress removeAction;
    private final String originalItemId;
    private final boolean skipReplacement;

    private static final ResourceLocation ITEM_BOX_TEX = new ResourceLocation(Oneenoughitem.MODID, "textures/gui/item_box.png");
    private static final ResourceLocation CROSS_TEX = new ResourceLocation(Oneenoughitem.MODID, "textures/gui/cross.png");

    public ItemDisplayWidget(int x, int y, ItemStack itemStack, Button.OnPress removeAction) {
        this(x, y, itemStack, removeAction, null, false);
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
        graphics.blit(ITEM_BOX_TEX, this.getX(), this.getY(), 0, 0, this.width, this.height, 18, 18);

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
            int crossX = this.getX() + this.width - 9;
            int crossY = this.getY() + 1;
            graphics.blit(CROSS_TEX, crossX, crossY, 0, 0, 8, 8, 8, 8);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.isHovered() && !this.itemStack.isEmpty()) {
            renderToolTip(graphics, mouseX, mouseY);
        }
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.isHovered() || this.itemStack.isEmpty()) return;

        renderToolTip(graphics, mouseX, mouseY);
    }

    private void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
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
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered() && this.removeAction != null) {
            int crossX = this.getX() + this.width - 9;
            int crossY = this.getY() + 1;
            if (mouseX >= crossX && mouseX <= crossX + 8 &&
                    mouseY >= crossY && mouseY <= crossY + 8) {

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