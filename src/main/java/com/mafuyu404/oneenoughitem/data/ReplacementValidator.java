package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.DataValidator;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.resources.ResourceLocation;

public class ReplacementValidator implements DataValidator<Replacements> {

    @Override
    public ValidationResult validate(Replacements replacement, ResourceLocation source) {
        // 验证目标物品是否存在
        if (Utils.getItemById(replacement.resultItems()) == null) {
            return ValidationResult.failure("Target item '" + replacement.resultItems() + "' does not exist");
        }

        // 验证是否至少有一个有效的源物品
        boolean hasValidSource = false;
        int validSourceCount = 0;

        for (String matchItem : replacement.matchItems()) {
            if (matchItem.startsWith("#")) {
                // 处理标签
                String tagIdString = matchItem.substring(1);
                try {
                    ResourceLocation tagId = new ResourceLocation(tagIdString);
                    if (Utils.isTagExists(tagId)) {
                        var tagItems = Utils.getItemsOfTag(tagId);
                        if (!tagItems.isEmpty()) {
                            hasValidSource = true;
                            validSourceCount += tagItems.size();
                            Oneenoughitem.LOGGER.debug("Valid tag in {}: '{}' contains {} items",
                                    source, matchItem, tagItems.size());
                        } else {
                            Oneenoughitem.LOGGER.warn("Tag in {} is empty: '{}'",
                                    source, matchItem);
                        }
                    } else {
                        Oneenoughitem.LOGGER.warn("Invalid tag in {}: '{}' does not exist",
                                source, matchItem);
                    }
                } catch (Exception e) {
                    Oneenoughitem.LOGGER.error("Invalid tag format in {}: '{}'",
                            source, matchItem, e);
                }
            } else {
                // 处理普通物品
                if (Utils.getItemById(matchItem) != null) {
                    hasValidSource = true;
                    validSourceCount++;
                } else {
                    Oneenoughitem.LOGGER.warn("Invalid source item in {}: '{}' does not exist",
                            source, matchItem);
                }
            }
        }

        if (!hasValidSource) {
            return ValidationResult.failure("No valid source items found for target '" + replacement.resultItems() + "'");
        }

        Oneenoughitem.LOGGER.debug("Replacement in {} validated: {} source items -> {}",
                source, validSourceCount, replacement.resultItems());

        return ValidationResult.success();
    }
}