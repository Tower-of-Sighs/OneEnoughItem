
package com.mafuyu404.oneenoughitem.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mafuyu404.oneenoughitem.init.Config;
import com.mafuyu404.oneenoughitem.init.MixinUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    @Shadow
    @Final
    private String directory;

    private static final Map<String, MixinUtils.FieldRule> DIR_RULES = Map.of(
            "recipes", new MixinUtils.FieldRule(Set.of("item", "id"), true),
            "advancements", new MixinUtils.FieldRule(Set.of("item"), false),
            "predicates", new MixinUtils.FieldRule(Set.of("item"), false)
    );

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;", at = @At("RETURN"))
    private void oei$replaceItemIdsInJson(ResourceManager resourceManager,
                                          ProfilerFiller profiler,
                                          CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {
        // 0 不动、1 保留、2 完全替换
        Integer mode = Config.DATA_REWRITE_MODE.get();
        if (mode == null) mode = 0;

        MixinUtils.FieldRule baseRule = DIR_RULES.get(this.directory);
        if (baseRule == null) return;

        // 先扫描本次重载的映射，并在配方开始重载的时候覆盖映射，保证后面的反序列化阶段也能识别到本次的映射
        Map<String, String> currentItemMap = MixinUtils.ReplacementLoader.loadCurrentReplacements(resourceManager);
        if ("recipes".equals(this.directory)) {
            ReplacementCache.beginReloadOverride(currentItemMap);
        }

        if (mode == 0) return;

        Set<String> currentSourceIds = new HashSet<>(currentItemMap.keySet());

        if (currentItemMap.isEmpty() && !ReplacementCache.hasAnyMappings()) return;

        boolean keepMode = (mode == 1);
        MixinUtils.FieldRule effectiveRule = new MixinUtils.FieldRule(baseRule.keys(), !keepMode && baseRule.strict());

        Map<ResourceLocation, JsonElement> results = cir.getReturnValue();
        if (results == null || results.isEmpty()) return;

        int replacedFiles = 0;
        int droppedFiles = 0;
        final boolean fallbackEnabled = currentItemMap.isEmpty(); // 扫不到映射时 fallback 到旧缓存

        for (Iterator<Map.Entry<ResourceLocation, JsonElement>> it = results.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ResourceLocation, JsonElement> entry = it.next();
            JsonElement json = entry.getValue();
            if (json == null) continue;

            MixinUtils.ReplaceContext ctx = new MixinUtils.ReplaceContext(
                    this.directory, effectiveRule, currentItemMap, currentSourceIds
            );
            // fallback 到旧缓存
            ctx.allowCacheFallback = fallbackEnabled;

            try {
                JsonElement processed = replaceElement(json, ctx, keepMode);

                if (!keepMode && ctx.shouldDrop) {
                    it.remove();
                    droppedFiles++;
                    MixinUtils.LogHelper.logFileOperation("dropped", entry.getKey(), this.directory, ctx.lastMappingOrigin);
                    continue;
                }

                if (ctx.mutated && processed != null) {
                    entry.setValue(processed);
                    replacedFiles++;
                    MixinUtils.LogHelper.logFileOperation("rewritten", entry.getKey(), this.directory,
                            keepMode ? "KEEP" : "FULL", fallbackEnabled);
                }

            } catch (Exception e) {
                MixinUtils.LogHelper.logError("JSON rewrite", entry.getKey(), this.directory, e);
            }
        }

        if (replacedFiles > 0 || droppedFiles > 0) {
            MixinUtils.LogHelper.logSummary(this.directory, replacedFiles, droppedFiles,
                    keepMode ? "KEEP" : "FULL", currentItemMap.isEmpty());
        }
    }

    private JsonElement replaceElement(JsonElement element, MixinUtils.ReplaceContext ctx, boolean keepMode) {
        if (element == null || ctx.shouldDrop) return element;
        if (element.isJsonObject()) return replaceInObject(element.getAsJsonObject(), ctx, keepMode);
        if (element.isJsonArray()) return replaceInArray(element.getAsJsonArray(), ctx, keepMode);
        return element;
    }

    private JsonElement replaceInObject(JsonObject obj, MixinUtils.ReplaceContext ctx, boolean keepMode) {
        if (ctx.shouldDrop) return obj;

        for (String key : new HashSet<>(obj.keySet())) {
            JsonElement value = obj.get(key);
            if (value == null) continue;

            if (ctx.rule.keys().contains(key) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                MixinUtils.IdReplacer.tryReplaceId(obj, key, value.getAsString(), ctx, keepMode);
                if (ctx.shouldDrop) return obj;
                continue;
            }

            if (value.isJsonObject()) {
                obj.add(key, replaceInObject(value.getAsJsonObject(), ctx, keepMode));
            } else if (value.isJsonArray()) {
                obj.add(key, replaceInArray(value.getAsJsonArray(), ctx, keepMode));
            }
        }
        return obj;
    }

    private JsonElement replaceInArray(JsonArray array, MixinUtils.ReplaceContext ctx, boolean keepMode) {
        if (ctx.shouldDrop) return array;

        for (int i = 0; i < array.size(); i++) {
            JsonElement elt = array.get(i);
            if (elt == null) continue;

            if (elt.isJsonObject()) {
                array.set(i, replaceInObject(elt.getAsJsonObject(), ctx, keepMode));
            } else if (elt.isJsonArray()) {
                array.set(i, replaceInArray(elt.getAsJsonArray(), ctx, keepMode));
            } else if (elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isString()) {
                MixinUtils.IdReplacer.tryReplaceId(array, i, elt.getAsString(), ctx, keepMode);
                if (ctx.shouldDrop) return array;
            }
        }
        return array;
    }
}