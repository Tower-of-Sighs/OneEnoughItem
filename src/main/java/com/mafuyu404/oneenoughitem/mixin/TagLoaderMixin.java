package com.mafuyu404.oneenoughitem.mixin;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.MixinUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
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

import java.util.*;

@Mixin(TagLoader.class)
public abstract class TagLoaderMixin<T> {

    @Shadow
    @Final
    private String directory;

    private static final String ITEMS_TAG_DIR = "tags/items";

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;", at = @At("HEAD"))
    private void oei$beginOverrideForTags(ResourceManager resourceManager,
                                          CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        if (!ITEMS_TAG_DIR.equals(this.directory)) {
            return;
        }
        try {
            Map<String, String> currentItemMap = MixinUtils.ReplacementLoader.loadCurrentReplacements(resourceManager);
            if (!currentItemMap.isEmpty() && !ReplacementCache.hasReloadOverride()) {
                ReplacementCache.beginReloadOverride(currentItemMap);
            } // fuck you cache
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Tag rewrite: begin reload-override failed for directory {}", this.directory, e);
        }
    }

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;", at = @At("RETURN"))
    private void oei$replaceTagItems(ResourceManager resourceManager,
                                     CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        int mode = Config.TAG_REWRITE_MODE.get();
        if (mode == 0) {
            return;
        }

        if (!ITEMS_TAG_DIR.equals(this.directory)) {
            return;
        }

        Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags = cir.getReturnValue();
        if (tags == null || tags.isEmpty()) return;

        Map<String, String> currentItemMap = Collections.emptyMap();
        try {
            currentItemMap = MixinUtils.ReplacementLoader.loadCurrentReplacements(resourceManager);
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Tag rewrite: failed to load current replacements (will fallback to cache if empty)", e);
        }
        final boolean fallbackEnabled = currentItemMap.isEmpty();

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
                    iterator.remove();
                    dropped++;
                    touched = true;
                    Oneenoughitem.LOGGER.debug("Item tag rewrite: drop '{}' from {} (replaced by '{}')",
                            fromStr, tagId, mapped);
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
            Oneenoughitem.LOGGER.info("Item tags rewrite summary (mode={}): affectedTags={}, totalDropped={}",
                    mode, totalTags, totalDropped);
        }
    }
}