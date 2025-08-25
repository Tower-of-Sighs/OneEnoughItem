package com.mafuyu404.oneenoughitem.client.gui.editor;

import com.mafuyu404.oneenoughitem.client.gui.components.ItemDisplayWidget;
import com.mafuyu404.oneenoughitem.client.gui.components.ScrollablePanel;
import com.mafuyu404.oneenoughitem.client.gui.components.TagDisplayWidget;

import java.util.List;

public class PanelsLayoutHelper {

    private static final int ITEM_SIZE = 18;
    private static final int ITEM_SPACING = 2;
    private static final int CONTENT_OFFSET_Y = 2;
    private static final int ITEMS_PER_ROW = 10;

    private static final int TAG_COLS = 2;
    private static final int TAG_SPACING_X = 8;
    private static final int TAG_ROW_HEIGHT = 20;

    private static final int FOUR_ROWS_VISIBLE_HEIGHT = 4 * 20;

    public void rebuildPanels(ScrollablePanel matchPanel,
                              ScrollablePanel resultPanel,
                              List<ItemDisplayWidget> matchItemWidgets,
                              List<TagDisplayWidget> matchTagWidgets,
                              ItemDisplayWidget resultItemWidget,
                              TagDisplayWidget resultTagWidget) {
        matchPanel.clearWidgets();
        resultPanel.clearWidgets();

        int index = 0;
        for (ItemDisplayWidget widget : matchItemWidgets) {
            int row = index / ITEMS_PER_ROW;
            int col = index % ITEMS_PER_ROW;

            int x = matchPanel.getX() + col * (ITEM_SIZE + ITEM_SPACING);
            int y = matchPanel.getY() + CONTENT_OFFSET_Y + row * (ITEM_SIZE + ITEM_SPACING);

            widget.setPosition(x, y);
            matchPanel.addWidget(widget);
            index++;
        }

        int tagStartY = CONTENT_OFFSET_Y + ((matchItemWidgets.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW) * (ITEM_SIZE + ITEM_SPACING) + 10;
        index = 0;
        for (TagDisplayWidget widget : matchTagWidgets) {
            int row = index / TAG_COLS;
            int col = index % TAG_COLS;

            int widgetW = widget.getWidth();
            int widgetH = widget.getHeight();

            int x = matchPanel.getX() + col * (widgetW + TAG_SPACING_X);
            int y = matchPanel.getY() + tagStartY + row * (widgetH + 4);

            widget.setPosition(x, y);
            matchPanel.addWidget(widget);
            index++;
        }

        if (resultItemWidget != null) {
            resultItemWidget.setPosition(resultPanel.getX() + resultPanel.getWidth() / 2 - 9, resultPanel.getY() + 28);
            resultPanel.addWidget(resultItemWidget);
        }
        if (resultTagWidget != null) {
            resultTagWidget.setPosition(resultPanel.getX() + resultPanel.getWidth() / 2 - 35, resultPanel.getY() + 28);
            resultPanel.addWidget(resultTagWidget);
        }

        int visibleHeight = getVisibleHeight(matchItemWidgets, matchTagWidgets);

        matchPanel.setVisibleHeight(visibleHeight);
        matchPanel.updateWidgetPositions();
        resultPanel.updateWidgetPositions();
    }

    private static int getVisibleHeight(List<ItemDisplayWidget> matchItemWidgets, List<TagDisplayWidget> matchTagWidgets) {
        int itemsRows = (matchItemWidgets.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
        int itemsBottom = itemsRows > 0
                ? CONTENT_OFFSET_Y + (itemsRows - 1) * (ITEM_SIZE + ITEM_SPACING) + ITEM_SIZE
                : 0;

        int tagsBottom = 0;
        int tagRows = (matchTagWidgets.size() + TAG_COLS - 1) / TAG_COLS;
        if (tagRows > 0) {
            int tagStart = CONTENT_OFFSET_Y + itemsRows * (ITEM_SIZE + ITEM_SPACING) + 10;
            tagsBottom = tagStart + (tagRows - 1) * (TAG_ROW_HEIGHT + 4) + TAG_ROW_HEIGHT;
        }
        int contentHeight = Math.max(itemsBottom, tagsBottom);
        int visibleHeight = Math.min(contentHeight, FOUR_ROWS_VISIBLE_HEIGHT);
        return visibleHeight;
    }
}