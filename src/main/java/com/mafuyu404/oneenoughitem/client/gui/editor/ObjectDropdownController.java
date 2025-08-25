package com.mafuyu404.oneenoughitem.client.gui.editor;

import com.mafuyu404.oneenoughitem.client.gui.components.ScrollablePanel;
import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import com.mafuyu404.oneenoughitem.client.gui.util.GuiUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;

public final class ObjectDropdownController {
    private ObjectDropdownController() {
    }

    public static ScrollablePanel rebuildDropdownPanel(Button anchorButton,
                                                       boolean show,
                                                       ReplacementEditorManager manager,
                                                       List<Button> objectIndexButtons,
                                                       IntConsumer onSelectIndex,
                                                       Runnable onDeleteCurrent) {
        if (!show || manager.getObjectSize() <= 0 || anchorButton == null) {
            return null;
        }

        int startX = anchorButton.getX();
        int startY = anchorButton.getY() + anchorButton.getHeight() + 2;

        int rowHeight = 22;
        int visibleRows = Math.min(8, Math.max(1, manager.getObjectSize()));
        int dropdownWidth = 95;
        int dropdownHeight = visibleRows * rowHeight;

        ScrollablePanel panel = new ScrollablePanel(startX, startY, dropdownWidth, dropdownHeight);

        objectIndexButtons.clear();
        for (int i = 0; i < manager.getObjectSize(); i++) {
            final int index = i;
            String btnText = String.valueOf(i + 1);
            if (manager.getCurrentObjectIndex() == i) {
                btnText += " ✓";
            }

            Button indexButton = GuiUtils.createObjectDropdownButton(
                    Component.literal(btnText),
                    btn -> onSelectIndex.accept(index),
                    startX,
                    startY + i * rowHeight,
                    40, 20
            );
            indexButton.setX(panel.getX());
            indexButton.setY(panel.getY() + i * rowHeight);

            objectIndexButtons.add(indexButton);
            panel.addWidget(indexButton);
        }

        if (manager.getCurrentObjectIndex() >= 0) {
            int idx = manager.getCurrentObjectIndex();
            Button deleteButton = GuiUtils.createObjectDropdownButton(
                    Component.translatable("gui.oneenoughitem.object.delete"),
                    btn -> onDeleteCurrent.run(),
                    startX + 45,
                    startY + idx * rowHeight,
                    50, 20
            );
            deleteButton.setX(panel.getX() + 45);
            deleteButton.setY(panel.getY() + idx * rowHeight);

            objectIndexButtons.add(deleteButton);
            panel.addWidget(deleteButton);
        }

        return panel;
    }

    public static Component buildDropdownButtonText(boolean show, ReplacementEditorManager manager) {
        String base = show
                ? Component.translatable("gui.oneenoughitem.object.element_up").getString()
                : Component.translatable("gui.oneenoughitem.object.element_down").getString();

        if (manager.getCurrentObjectIndex() >= 0) {
            base += " (" + (manager.getCurrentObjectIndex() + 1) + "/" + manager.getObjectSize() + ")";
        }
        return Component.literal(base);
    }
}