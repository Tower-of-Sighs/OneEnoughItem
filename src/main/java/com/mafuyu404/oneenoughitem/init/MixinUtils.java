package com.mafuyu404.oneenoughitem.init;

import com.google.gson.*;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.*;
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
        public final Map<String, Replacements.Rules> itemRules;

        public ReplaceContext(String dataType, FieldRule rule,
                              Map<String, String> itemMap,
                              Set<String> sourceItemIds,
                              Map<String, Replacements.Rules> itemRules) {
            this.dataType = dataType;
            this.rule = rule;
            this.itemMap = itemMap;
            this.sourceItemIds = sourceItemIds;
            this.itemRules = itemRules;
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
                BiConsumer<String, String> logReplaceAction,
                Consumer<JsonElement> setValueAction
        ) {
            String target = TargetResolver.resolveTarget(id, ctx);
            if (target == null) return;

            // 先使用本次重载的规则判断是否应在该目录处理；若本次无规则且允许回落，再查缓存
            boolean shouldProcess = false;
            if (ctx.itemRules != null) {
                Replacements.Rules rules = ctx.itemRules.get(id);
                if (rules != null) {
                    shouldProcess = rules.data()
                            .map(m -> m.get(ctx.dataType))
                            .map(mode -> mode == Replacements.ProcessingMode.REPLACE)
                            .orElse(false);
                } else if (ctx.allowCacheFallback) {
                    shouldProcess = ReplacementCache.shouldReplaceInDataDir(id, ctx.dataType);
                }
            } else if (ctx.allowCacheFallback) {
                shouldProcess = ReplacementCache.shouldReplaceInDataDir(id, ctx.dataType);
            }
            if (!shouldProcess) {
                return; // 跳过不需要处理的物品
            }

            if ("minecraft:air".equals(target)) {
                ctx.shouldDrop = true;
                LogHelper.logDrop(ctx.dataType, id, target, ctx.lastMappingOrigin, "mapping");
                return;
            }

            if (!target.equals(id)) {
                setValueAction.accept(new JsonPrimitive(target));
                ctx.mutated = true;
                logReplaceAction.accept(id, target);
            }

            if (ctx.rule.strict() && TargetResolver.isSourceIdWithCurrent(id, ctx)) {
                ctx.shouldDrop = true;
                LogHelper.logDrop(ctx.dataType, id, ctx.lastMappingOrigin, ctx.lastMappingOrigin, "strict residual source-id");
            }
        }

        public static void tryReplaceId(JsonObject obj, String key, String id, ReplaceContext ctx) {
            replaceIdCommon(
                    id,
                    ctx,
                    (oldId, newId) -> LogHelper.logReplace(ctx.dataType, oldId, newId, ctx.lastMappingOrigin, false),
                    value -> obj.add(key, value)
            );
        }

        public static void tryReplaceId(JsonArray array, int index, String id, ReplaceContext ctx) {
            replaceIdCommon(
                    id,
                    ctx,
                    (oldId, newId) -> LogHelper.logReplace(ctx.dataType, oldId, newId, ctx.lastMappingOrigin, true),
                    value -> array.set(index, value)
            );
        }
    }

    public static class ReplacementLoader {
        private static final List<String> REPLACEMENT_DIR_CANDIDATES = List.of("replacements");

        // 重载快照：物品映射 + 规则
        public record CurrentSnapshot(
                Map<String, String> itemMap,
                Map<String, Replacements.Rules> itemRules,
                Map<String, Replacements.Rules> tagRules
        ) {
        }

        public static CurrentSnapshot loadCurrentSnapshot(ResourceManager resourceManager) {
            Map<String, String> map = new HashMap<>();
            Map<String, Replacements.Rules> itemRules = new HashMap<>();
            Map<String, Replacements.Rules> tagRules = new HashMap<>();
            Predicate<ResourceLocation> jsonPredicate = rl -> rl.getPath().endsWith(".json") && "oei".equals(rl.getNamespace());

            for (String baseDir : REPLACEMENT_DIR_CANDIDATES) {
                try {
                    Map<ResourceLocation, Resource> res = resourceManager.listResources(baseDir, jsonPredicate);
                    if (res.isEmpty()) continue;

                    for (Map.Entry<ResourceLocation, Resource> e : res.entrySet()) {
                        try (Reader reader = e.getValue().openAsReader()) {
                            JsonElement root = JsonParser.parseReader(reader);
                            parseReplacementJson(root, map, itemRules, tagRules);
                        } catch (Exception ex) {
                            LogHelper.logParseError(e.getKey(), ex);
                        }
                    }
                } catch (Exception ignore) {
                }
            }
            return new CurrentSnapshot(map, itemRules, tagRules);
        }

        private static void parseReplacementJson(JsonElement root,
                                                 Map<String, String> outMap,
                                                 Map<String, Replacements.Rules> outItemRules,
                                                 Map<String, Replacements.Rules> outTagRules) {
            if (root == null) return;

            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    parseReplacementJson(el, outMap, outItemRules, outTagRules);
                }
                return;
            }

            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("matchItems") && obj.get("matchItems").isJsonArray() && obj.has("resultItems")) {
                    String result = obj.get("resultItems").getAsString();
                    JsonArray arr = obj.get("matchItems").getAsJsonArray();

                    Replacements.Rules rules = null;
                    if (obj.has("rules") && obj.get("rules").isJsonObject()) {
                        rules = parseRulesObject(obj.getAsJsonObject("rules"));
                    }

                    for (JsonElement el : arr) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            String src = el.getAsString();
                            if (src != null && !src.isEmpty()) {
                                if (src.startsWith("#")) {
                                    String tagId = src.substring(1);
                                    if (rules != null) {
                                        outTagRules.put(tagId, rules);
                                    }
                                    outMap.put(src, result);
                                } else {
                                    outMap.put(src, result);
                                    if (rules != null) {
                                        outItemRules.put(src, rules);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 支持将多个对象放在一个文件的情况
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        parseReplacementJson(e.getValue(), outMap, outItemRules, outTagRules);
                    }
                }
            }
        }

        private static Replacements.Rules parseRulesObject(JsonObject rulesObj) {
            Optional<Map<String, Replacements.ProcessingMode>> dataOpt = Optional.empty();
            Optional<Map<String, Replacements.ProcessingMode>> tagOpt = Optional.empty();

            if (rulesObj.has("data") && rulesObj.get("data").isJsonObject()) {
                Map<String, Replacements.ProcessingMode> data = parseModeMap(rulesObj.getAsJsonObject("data"));
                dataOpt = data.isEmpty() ? Optional.empty() : Optional.of(data);
            }

            if (rulesObj.has("tag") && rulesObj.get("tag").isJsonObject()) {
                Map<String, Replacements.ProcessingMode> tag = parseModeMap(rulesObj.getAsJsonObject("tag"));
                tagOpt = tag.isEmpty() ? Optional.empty() : Optional.of(tag);
            }

            if (dataOpt.isEmpty() && tagOpt.isEmpty()) {
                return null;
            }

            return new Replacements.Rules(dataOpt, tagOpt);
        }

        private static Map<String, Replacements.ProcessingMode> parseModeMap(JsonObject obj) {
            Map<String, Replacements.ProcessingMode> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    String stringValue = value.getAsString();
                    try {
                        Replacements.ProcessingMode mode = Replacements.ProcessingMode.valueOf(stringValue.toUpperCase());
                        map.put(entry.getKey(), mode);
                    } catch (IllegalArgumentException ignored) {
                        // 跳过无法识别的值
                    }
                }
            }
            return map;
        }
    }

    public static FieldRule getDataDirFieldRule(String directory) {
        return switch (directory) {
            case "recipes" -> new FieldRule(Set.of("item", "id", "result"), true);
            case "advancements" -> new FieldRule(Set.of("item"), true);
            case "loot_tables" -> new FieldRule(Set.of("name"), true);
            default -> new FieldRule(Set.of("item", "id", "result"), false);
        };
    }
}