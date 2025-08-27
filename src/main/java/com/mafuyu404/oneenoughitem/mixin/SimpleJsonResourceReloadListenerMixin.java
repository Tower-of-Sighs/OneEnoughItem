package com.mafuyu404.oneenoughitem.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.MixinUtils;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    @Shadow
    @Final
    private String directory;

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;", at = @At("RETURN"))
    private void oei$replaceItemIdsInJson(ResourceManager resourceManager,
                                          ProfilerFiller profiler,
                                          CallbackInfoReturnable<Map<ResourceLocation, JsonElement>> cir) {

        // 获取数据目录的字段规则
        MixinUtils.FieldRule baseRule = MixinUtils.getDataDirFieldRule(this.directory);

        // 扫描本次重载的映射 + 规则；若是配方目录则启用临时覆盖映射
        MixinUtils.ReplacementLoader.CurrentSnapshot snapshot = MixinUtils.ReplacementLoader.loadCurrentSnapshot(resourceManager);
        Map<String, String> currentItemMap = snapshot.itemMap();
        Map<String, Replacements.Rules> currentItemRules = snapshot.itemRules();
        if ("recipes".equals(this.directory)) {
            ReplacementCache.beginReloadOverride(currentItemMap);
        }

        Set<String> currentSourceIds = new HashSet<>(currentItemMap.keySet());

        if (currentItemMap.isEmpty() && !ReplacementCache.hasAnyMappings()) return;

        MixinUtils.FieldRule effectiveRule = new MixinUtils.FieldRule(baseRule.keys(), baseRule.strict());

        Map<String, Replacements.Rules> effectiveItemRules = getStringRulesMap(currentItemRules, currentItemMap);

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
                    this.directory, effectiveRule, currentItemMap, currentSourceIds, effectiveItemRules
            );
            // fallback 到旧缓存（同时影响规则与映射）
            ctx.allowCacheFallback = fallbackEnabled;

            try {
                JsonElement processed = replaceElement(json, ctx);

                if (ctx.shouldDrop) {
                    it.remove();
                    droppedFiles++;
                    MixinUtils.LogHelper.logFileOperation("dropped", entry.getKey(), this.directory, ctx.lastMappingOrigin);
                    continue;
                }

                if (ctx.mutated && processed != null) {
                    entry.setValue(processed);
                    replacedFiles++;
                    MixinUtils.LogHelper.logFileOperation("rewritten", entry.getKey(), this.directory,
                            "RULE-BASED", fallbackEnabled);
                }

            } catch (Exception e) {
                MixinUtils.LogHelper.logError("JSON rewrite", entry.getKey(), this.directory, e);
            }
        }

        if (replacedFiles > 0 || droppedFiles > 0) {
            MixinUtils.LogHelper.logSummary(this.directory, replacedFiles, droppedFiles,
                    "RULE-BASED", currentItemMap.isEmpty());
        }
    }

    private static @NotNull Map<String, Replacements.Rules> getStringRulesMap(Map<String, Replacements.Rules> currentItemRules, Map<String, String> currentItemMap) {
        Replacements.Rules defaultRules = null;
        try {
            var cfg = Config.DEFAULT_RULES.getValue();
            if (cfg != null) {
                defaultRules = cfg.toRules();
            }
        } catch (Exception ignored) {
        }

        Map<String, Replacements.Rules> effectiveItemRules =
                new HashMap<>(currentItemRules);

        if (defaultRules != null) {
            for (String from : currentItemMap.keySet()) {
                effectiveItemRules.putIfAbsent(from, defaultRules);
            }
        }
        return effectiveItemRules;
    }

    private JsonElement replaceElement(JsonElement element, MixinUtils.ReplaceContext ctx) {
        if (element == null || ctx.shouldDrop) return element;
        if (element.isJsonObject()) return replaceInObject(element.getAsJsonObject(), ctx);
        if (element.isJsonArray()) return replaceInArray(element.getAsJsonArray(), ctx);
        return element;
    }

    private JsonElement replaceInObject(JsonObject obj, MixinUtils.ReplaceContext ctx) {
        if (ctx.shouldDrop) return obj;

        for (String key : new HashSet<>(obj.keySet())) {
            JsonElement value = obj.get(key);
            if (value == null) continue;

            if (ctx.rule.keys().contains(key) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                MixinUtils.IdReplacer.tryReplaceId(obj, key, value.getAsString(), ctx);
                if (ctx.shouldDrop) return obj;
                continue;
            }

            if (value.isJsonObject()) {
                obj.add(key, replaceInObject(value.getAsJsonObject(), ctx));
            } else if (value.isJsonArray()) {
                obj.add(key, replaceInArray(value.getAsJsonArray(), ctx));
            }
        }
        return obj;
    }

    private JsonElement replaceInArray(JsonArray array, MixinUtils.ReplaceContext ctx) {
        if (ctx.shouldDrop) return array;

        for (int i = 0; i < array.size(); i++) {
            JsonElement elt = array.get(i);
            if (elt == null) continue;

            if (elt.isJsonObject()) {
                array.set(i, replaceInObject(elt.getAsJsonObject(), ctx));
            } else if (elt.isJsonArray()) {
                array.set(i, replaceInArray(elt.getAsJsonArray(), ctx));
            } else if (elt.isJsonPrimitive() && elt.getAsJsonPrimitive().isString()) {
                MixinUtils.IdReplacer.tryReplaceId(array, i, elt.getAsString(), ctx);
                if (ctx.shouldDrop) return array;
            }
        }
        return array;
    }
}