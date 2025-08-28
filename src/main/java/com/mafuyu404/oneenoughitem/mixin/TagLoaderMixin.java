package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.MixinUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin<T> {

    @Shadow
    @Final
    private String directory;

    private static final String ITEMS_TAG_DIR = "tags/item";

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;", at = @At("HEAD"))
    private void oei$beginOverrideForTags(ResourceManager resourceManager,
                                          CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (!ITEMS_TAG_DIR.equals(this.directory)) {
            return;
        }
        try {
            MixinUtils.ReplacementLoader.CurrentSnapshot snapshot = MixinUtils.ReplacementLoader.loadCurrentSnapshot(resourceManager);
            Map<String, String> currentItemMap = snapshot.itemMap();
            if (!currentItemMap.isEmpty() && !ReplacementCache.hasReloadOverride()) {
                ReplacementCache.beginReloadOverride(currentItemMap);
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Tag rewrite: begin reload-override failed for directory {}", this.directory, e);
        }
    }

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;", at = @At("RETURN"))
    private void oei$replaceTagItems(ResourceManager resourceManager,
                                     CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {

        String tagType = getTagType(this.directory);
        if (tagType == null) {
            return; // 只处理已知的标签类型
        }

        Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags = cir.getReturnValue();
        if (tags == null || tags.isEmpty()) return;

        Map<String, String> currentItemMap = Collections.emptyMap();
        Map<String, Replacements.Rules> currentItemRules = Collections.emptyMap();
        try {
            MixinUtils.ReplacementLoader.CurrentSnapshot snapshot = MixinUtils.ReplacementLoader.loadCurrentSnapshot(resourceManager);
            currentItemMap = snapshot.itemMap();
            currentItemRules = snapshot.itemRules();
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Tag rewrite: failed to load current replacements (will fallback to cache if empty)", e);
        }
        final boolean fallbackEnabled = currentItemMap.isEmpty();

        Replacements.Rules defaultRules = null;
        try {
            var cfg = ModConfig.DEFAULT_RULES.getValue();
            if (cfg != null) {
                defaultRules = cfg.toRules();
            }
        } catch (Exception ignored) {
        }

        int totalTags = 0, totalDropped = 0;

        for (Map.Entry<ResourceLocation, List<TagLoader.EntryWithSource>> tagEntry : tags.entrySet()) {
            ResourceLocation tagId = tagEntry.getKey();
            List<TagLoader.EntryWithSource> entries = tagEntry.getValue();
            if (entries == null || entries.isEmpty()) continue;

            Iterator<TagLoader.EntryWithSource> iterator = entries.iterator();
            int dropped = 0;
            boolean touched = false;

            while (iterator.hasNext()) {
                TagLoader.EntryWithSource tracked = iterator.next();
                TagEntry e = tracked.entry();

                if (e.isTag()) continue;

                ResourceLocation fromId = e.getId();
                String fromStr = fromId.toString();

                String mapped = currentItemMap.get(fromStr);
                if (mapped == null && fallbackEnabled) {
                    mapped = ReplacementCache.matchItem(fromStr);
                }

                if (mapped != null) {
                    boolean shouldReplace = false;

                    Replacements.Rules rules = currentItemRules.get(fromStr);
                    if (rules == null) {
                        rules = defaultRules;
                    }

                    if (rules != null) {
                        shouldReplace = rules.tag()
                                .map(m -> m.get(tagType))
                                .map(mode -> mode == Replacements.ProcessingMode.REPLACE)
                                .orElse(false);
                    } else if (fallbackEnabled) {
                        // 若当前映射为空且无默认规则，维持原有回退逻辑
                        shouldReplace = ReplacementCache.shouldReplaceInTagType(fromStr, tagType);
                    }

                    if (shouldReplace) {
                        iterator.remove();
                        dropped++;
                        touched = true;
                        Oneenoughitem.LOGGER.debug("Item tag rewrite: drop '{}' from {} (replaced by '{}', rule={})",
                                fromStr, tagId, mapped, tagType);
                    }
                }
            }

            if (touched) {
                totalTags++;
                totalDropped += dropped;
                Oneenoughitem.LOGGER.info("Item tag rewrite: {} -> dropped={}",
                        tagId, dropped);
            }
        }

        if (totalTags > 0) {
            Oneenoughitem.LOGGER.info("Item tags rewrite summary (rule-based): affectedTags={}, totalDropped={}",
                    totalTags, totalDropped);
        }
    }

    private String getTagType(String directory) {
        return switch (directory) {
            case "tags/item" -> "item";
            case "tags/block" -> "block";
            case "tags/fluid" -> "fluid";
            default -> null;
        };
    }
}