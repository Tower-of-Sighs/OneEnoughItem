package com.mafuyu404.oneenoughitem.client.gui.editor;

import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ItemsController {
    private final ReplacementEditorManager manager;
    private final Runnable uiSync;

    public ItemsController(ReplacementEditorManager manager, Runnable uiSync) {
        this.manager = manager;
        this.uiSync = uiSync;
    }

    public void addMatchItem(Item item) {
        this.manager.addMatchItem(item);
        this.uiSync.run();
    }

    public void addMatchTag(ResourceLocation tagId) {
        this.manager.addMatchTag(tagId);
        this.uiSync.run();
    }

    public boolean removeMatchItem(Item item) {
        boolean removed = this.manager.removeMatchItem(item);
        if (removed) {
            this.uiSync.run();
        }
        return removed;
    }

    public void removeMatchTag(ResourceLocation tagId) {
        this.manager.removeMatchTag(tagId);
        this.uiSync.run();
    }

    public void setResultItem(Item item) {
        this.manager.setResultItem(item);
        this.uiSync.run();
    }

    public void setResultTag(ResourceLocation tagId) {
        this.manager.setResultTag(tagId);
        this.uiSync.run();
    }

    public void clearMatchItems() {
        this.manager.clearMatchItems();
        this.uiSync.run();
    }

    public void clearResultItem() {
        this.manager.clearResultItem();
        this.uiSync.run();
    }
}