package com.mafuyu404.oneenoughitem.data;

import com.mafuyu404.oelib.api.DataValidator;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;

public class ReplacementValidator implements DataValidator.ServerContextAware<Replacements> {

    @Override
    public ValidationResult validateWithContext(Replacements replacement, ResourceLocation source, MinecraftServer server) {
        // 验证目标物品是否存在
        if (Utils.getItemById(replacement.resultItems()) == null) {
            return ValidationResult.failure("Target item '" + replacement.resultItems() + "' does not exist");
        }

        // 如果没有服务器上下文，只能进行基础验证
        if (server == null) {
            Oneenoughitem.LOGGER.debug("No server context available for validation of {}, performing basic validation only", source);
            return ValidationResult.success();
        }

        // 获取注册表查找器
        HolderLookup.RegistryLookup<Item> registryLookup = server.registryAccess().lookupOrThrow(Registries.ITEM);

        // 验证是否至少有一个有效的源物品
        boolean hasValidSource = false;
        int validSourceCount = 0;

        for (String matchItem : replacement.matchItems()) {
            if (matchItem.startsWith("#")) {
                // 处理标签
                String tagIdString = matchItem.substring(1);
                try {
                    ResourceLocation tagId = ResourceLocation.parse(tagIdString);
                    var tagItems = Utils.getItemsOfTag(tagId, registryLookup);
                    if (!tagItems.isEmpty()) {
                        hasValidSource = true;
                        validSourceCount += tagItems.size();
                        Oneenoughitem.LOGGER.debug("Valid tag in {}: '{}' contains {} items",
                                source, matchItem, tagItems.size());
                    } else {
                        Oneenoughitem.LOGGER.warn("Tag in {} is empty: '{}'",
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