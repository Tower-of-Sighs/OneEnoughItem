package com.mafuyu404.oneenoughitem.init;

import com.google.gson.*;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MixinUtils {
    public record FieldRule(Set<String> keys, boolean strict) {
    }

    public static final class ReplaceContext {
        public final String dataType;
        public final FieldRule rule;
        public final Map<String, String> itemMap;
        public final Set<String> sourceItemIds;
        public boolean mutated = false;
        public boolean shouldDrop = false;
        public boolean allowCacheFallback;
        public String lastMappingOrigin = "N/A";

        public ReplaceContext(String dataType, FieldRule rule,
                              Map<String, String> itemMap,
                              Set<String> sourceItemIds) {
            this.dataType = dataType;
            this.rule = rule;
            this.itemMap = itemMap;
            this.sourceItemIds = sourceItemIds;
            this.allowCacheFallback = (itemMap == null || itemMap.isEmpty());
        }
    }

    public static class LogHelper {
        public static void logDrop(String directory, String id, String target, String origin, String reason) {
            Oneenoughitem.LOGGER.debug("Drop by {} in {}: {} -> {} (origin={})",
                    reason, directory, id, target, origin);
        }

        public static void logReplace(String directory, String id, String target, String origin, boolean isArray) {
            String prefix = isArray ? "Replaced array id" : "Replaced id";
            Oneenoughitem.LOGGER.debug("{} in {}: {} -> {} (origin={})",
                    prefix, directory, id, target, origin);
        }

        public static void logFileOperation(String operation, ResourceLocation key, String directory, Object... params) {
            switch (operation) {
                case "dropped" -> Oneenoughitem.LOGGER.debug("Dropped {} in {} (origin={}, reason=rule)",
                        key, directory, params[0]);
                case "rewritten" -> Oneenoughitem.LOGGER.debug("Rewritten {} in {} (mode={}, fallbackCache={})",
                        key, directory, params[0], params[1]);
            }
        }

        public static void logSummary(String directory, int replaced, int dropped, String mode, boolean fallbackCache) {
            Oneenoughitem.LOGGER.debug("OEI JSON rewrite in '{}': replaced={}, dropped={}, mode={}, fallbackCache={}",
                    directory, replaced, dropped, mode, fallbackCache);
        }

        public static void logError(String operation, ResourceLocation key, String directory, Exception e) {
            Oneenoughitem.LOGGER.debug("OEI {} failed for {} in '{}'", operation, key, directory, e);
        }

        public static void logParseError(ResourceLocation key, Exception ex) {
            Oneenoughitem.LOGGER.debug("Failed parsing replacements at {}: {}", key, ex.toString());
        }
    }

    public static class TargetResolver {
        public static String resolveTarget(String id, ReplaceContext ctx) {
            if (ctx.itemMap != null) {
                String v = ctx.itemMap.get(id);
                if (v != null) {
                    ctx.lastMappingOrigin = "CURRENT";
                    return v;
                }
            }
            if (ctx.allowCacheFallback) {
                String v = ReplacementCache.matchItem(id);
                if (v != null) {
                    ctx.lastMappingOrigin = "CACHE";
                    return v;
                }
            }
            ctx.lastMappingOrigin = "N/A";
            return null;
        }

        public static boolean isSourceIdWithCurrent(String id, ReplaceContext ctx) {
            if (ctx.sourceItemIds != null && ctx.sourceItemIds.contains(id)) return true;
            return ctx.allowCacheFallback && ReplacementCache.isSourceItemId(id);
        }
    }

    public static class IdReplacer {
        private static void replaceIdCommon(
                String id,
                ReplaceContext ctx,
                boolean keepMode,
                BiConsumer<String, String> logReplaceAction,
                Consumer<JsonElement> setValueAction
        ) {
            String target = TargetResolver.resolveTarget(id, ctx);
            if (target == null) return;

            if (!keepMode && "minecraft:air".equals(target)) {
                ctx.shouldDrop = true;
                LogHelper.logDrop(ctx.dataType, id, target, ctx.lastMappingOrigin, "mapping");
                return;
            }

            if (!target.equals(id)) {
                setValueAction.accept(new JsonPrimitive(target));
                ctx.mutated = true;
                logReplaceAction.accept(id, target);
            }

            if (!keepMode && ctx.rule.strict() && TargetResolver.isSourceIdWithCurrent(id, ctx)) {
                ctx.shouldDrop = true;
                LogHelper.logDrop(ctx.dataType, id, ctx.lastMappingOrigin, ctx.lastMappingOrigin, "strict residual source-id");
            }
        }

        public static void tryReplaceId(JsonObject obj, String key, String id, ReplaceContext ctx, boolean keepMode) {
            replaceIdCommon(
                    id,
                    ctx,
                    keepMode,
                    (oldId, newId) -> LogHelper.logReplace(ctx.dataType, oldId, newId, ctx.lastMappingOrigin, false),
                    value -> obj.add(key, value)
            );
        }

        public static void tryReplaceId(JsonArray array, int index, String id, ReplaceContext ctx, boolean keepMode) {
            replaceIdCommon(
                    id,
                    ctx,
                    keepMode,
                    (oldId, newId) -> LogHelper.logReplace(ctx.dataType, oldId, newId, ctx.lastMappingOrigin, true),
                    value -> array.set(index, value)
            );
        }
    }

    public static class ReplacementLoader {
        private static final List<String> REPLACEMENT_DIR_CANDIDATES = List.of("replacements");

        public static Map<String, String> loadCurrentReplacements(ResourceManager resourceManager) {
            Map<String, String> map = new HashMap<>();
            Predicate<ResourceLocation> jsonPredicate = rl -> rl.getPath().endsWith(".json") && "oei".equals(rl.getNamespace());

            for (String baseDir : REPLACEMENT_DIR_CANDIDATES) {
                try {
                    Map<ResourceLocation, Resource> res = resourceManager.listResources(baseDir, jsonPredicate);
                    if (res.isEmpty()) continue;

                    for (Map.Entry<ResourceLocation, Resource> e : res.entrySet()) {
                        try (Reader reader = e.getValue().openAsReader()) {
                            JsonElement root = JsonParser.parseReader(reader);
                            parseReplacementJson(root, map);
                        } catch (Exception ex) {
                            LogHelper.logParseError(e.getKey(), ex);
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            return map;
        }

        private static void parseReplacementJson(JsonElement root, Map<String, String> out) {
            if (root == null) return;

            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    parseReplacementJson(el, out);
                }
                return;
            }

            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("matchItems") && obj.get("matchItems").isJsonArray() && obj.has("resultItems")) {
                    String result = obj.get("resultItems").getAsString();
                    JsonArray arr = obj.get("matchItems").getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            String src = el.getAsString();
                            if (src != null && !src.isEmpty() && !src.startsWith("#")) {
                                out.put(src, result);
                            }
                        }
                    }
                } else {
                    // 支持将多个对象放在一个文件的情况
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        parseReplacementJson(e.getValue(), out);
                    }
                }
            }
        }
    }
}