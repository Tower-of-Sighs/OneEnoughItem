package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.resources.ResourceLocation;

public final class Validators {
    private Validators() {}

    public static ValidationStreams.Accumulator fromItem(String itemId, ResourceLocation source) {
        if (itemId.startsWith("#")) {
            return fromTag(itemId.substring(1), source);
        }
        return Utils.getItemById(itemId) != null
                ? ValidationStreams.Accumulator.valid(1)
                : ValidationStreams.Accumulator.invalid();
    }

    public static ValidationStreams.Accumulator fromTag(String tagIdString, ResourceLocation source) {
        try {
            ResourceLocation tagId = new ResourceLocation(tagIdString);
            if (Utils.isTagExists(tagId)) {
                var tagItems = Utils.getItemsOfTag(tagId);
                if (!tagItems.isEmpty()) {
                    Oneenoughitem.LOGGER.debug("Valid tag in {}: '{}' contains {} items", source, tagId, tagItems.size());
                    return ValidationStreams.Accumulator.valid(tagItems.size());
                } else {
                    Oneenoughitem.LOGGER.warn("Tag in {} is empty: '{}'", source, tagId);
                    return ValidationStreams.Accumulator.invalid();
                }
            } else {
                Oneenoughitem.LOGGER.debug("Tag in {} not found (may be uninitialized): '{}'", source, tagId);
                return ValidationStreams.Accumulator.deferred();
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Invalid tag format in {}: '{}'", source, tagIdString, e);
            return ValidationStreams.Accumulator.failure("Invalid tag format: " + tagIdString);
        }
    }
}
