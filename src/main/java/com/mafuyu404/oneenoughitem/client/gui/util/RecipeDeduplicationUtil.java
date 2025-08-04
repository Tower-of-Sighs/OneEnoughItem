package com.mafuyu404.oneenoughitem.client.gui.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

public class RecipeDeduplicationUtil {

    public static class DeduplicationResult {
        private final boolean success;
        private final String message;
        private final ChatFormatting formatting;
        private final int recipeCount;
        private final Path scriptPath;

        private DeduplicationResult(boolean success, String message, ChatFormatting formatting, int recipeCount, Path scriptPath) {
            this.success = success;
            this.message = message;
            this.formatting = formatting;
            this.recipeCount = recipeCount;
            this.scriptPath = scriptPath;
        }

        public static DeduplicationResult success(String message, int recipeCount, Path scriptPath) {
            return new DeduplicationResult(true, message, ChatFormatting.GREEN, recipeCount, scriptPath);
        }

        public static DeduplicationResult warning(String message) {
            return new DeduplicationResult(false, message, ChatFormatting.YELLOW, 0, null);
        }

        public static DeduplicationResult error(String message) {
            return new DeduplicationResult(false, message, ChatFormatting.RED, 0, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ChatFormatting getFormatting() {
            return formatting;
        }

        public int getRecipeCount() {
            return recipeCount;
        }

        public Path getScriptPath() {
            return scriptPath;
        }
    }

    public static DeduplicationResult deduplicateRecipes(Path targetFilePath, Consumer<Component> messageCallback) {
        if (!CompatUtil.isKubeJSLoaded()) {
            return DeduplicationResult.error("error.oneenoughitem.kubejs_not_loaded");
        }

        if (targetFilePath == null) {
            return DeduplicationResult.error("error.oneenoughitem.no_file_selected");
        }

        try {
            Set<String> allMatchItems = new HashSet<>();
            Set<String> allMatchTags = new HashSet<>();

            collectMatchItemsFromFile(targetFilePath, allMatchItems, allMatchTags);

            if (allMatchItems.isEmpty() && allMatchTags.isEmpty()) {
                return DeduplicationResult.warning("warning.oneenoughitem.no_match_items");
            }

            Path scriptFile = createScriptDirectory();
            if (scriptFile == null) {
                return DeduplicationResult.error("error.oneenoughitem.script_directory_creation_failed");
            }

            List<String> recipeIds = collectRecipeIds(new ArrayList<>(allMatchItems), new ArrayList<>(allMatchTags));

            if (recipeIds.isEmpty()) {
                return DeduplicationResult.warning("warning.oneenoughitem.no_recipes_found");
            }

            String scriptContent = generateOptimizedScript(recipeIds, allMatchItems.size(), allMatchTags.size(), targetFilePath.getFileName().toString());

            try {
                Files.writeString(scriptFile, scriptContent);
                if (messageCallback != null) {
                    messageCallback.accept(Component.translatable("message.oneenoughitem.reload_required")
                            .withStyle(ChatFormatting.YELLOW));
                }

                Oneenoughitem.LOGGER.info("Generated KubeJS script with {} recipe removals for file {} at: {}",
                        recipeIds.size(), targetFilePath.getFileName(), scriptFile);

                return DeduplicationResult.success("message.oneenoughitem.script_generated", recipeIds.size(), scriptFile);

            } catch (IOException e) {
                Oneenoughitem.LOGGER.error("Failed to write KubeJS script file", e);
                return DeduplicationResult.error("error.oneenoughitem.script_write_failed");
            }

        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to generate recipe removal script", e);
            return DeduplicationResult.error("error.oneenoughitem.script_generation_failed");
        }
    }

    private static void collectMatchItemsFromFile(Path filePath, Set<String> allMatchItems, Set<String> allMatchTags) {
        try {
            if (!Files.exists(filePath)) {
                Oneenoughitem.LOGGER.warn("Target file does not exist: {}", filePath);
                return;
            }

            String content = Files.readString(filePath);
            if (content.trim().isEmpty()) {
                return;
            }

            JsonElement parsed = new Gson().fromJson(content, JsonElement.class);
            if (parsed != null && parsed.isJsonArray()) {
                JsonArray jsonArray = parsed.getAsJsonArray();
                for (JsonElement element : jsonArray) {
                    processMatchItemsFromElement(element, allMatchItems, allMatchTags);
                }
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to read replacement file: {}", filePath, e);
        }

        Oneenoughitem.LOGGER.info("Collected {} items and {} tags from file: {}",
                allMatchItems.size(), allMatchTags.size(), filePath.getFileName());
    }


    private static void processMatchItemsFromElement(JsonElement element, Set<String> allMatchItems, Set<String> allMatchTags) {
        try {
            var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);

            if (result.result().isPresent()) {
                var replacement = result.result().get();

                for (String matchItem : replacement.matchItems()) {
                    if (matchItem.startsWith("#")) {
                        // 标签，展开为具体物品
                        String tagIdString = matchItem.substring(1);
                        try {
                            ResourceLocation tagId = new ResourceLocation(tagIdString);
                            Collection<Item> tagItems = Utils.getItemsOfTag(tagId);

                            if (tagItems.isEmpty()) {
                                Oneenoughitem.LOGGER.warn("Tag {} is empty or not found", tagId);
                                allMatchTags.add(matchItem); // 保留原标签用于统计
                            } else {
                                for (Item item : tagItems) {
                                    String itemId = Utils.getItemRegistryName(item);
                                    if (itemId != null) {
                                        allMatchItems.add(itemId);
                                    }
                                }
                                allMatchTags.add(matchItem); // 保留原标签用于统计
                                Oneenoughitem.LOGGER.info("Expanded tag {} to {} items", tagId, tagItems.size());
                            }
                        } catch (Exception e) {
                            Oneenoughitem.LOGGER.error("Invalid tag ID format: {}", matchItem, e);
                            allMatchTags.add(matchItem); // 保留原标签用于统计
                        }
                    } else {
                        allMatchItems.add(matchItem);
                    }
                }
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to parse replacement element: {}", element, e);
        }
    }

    private static Path createScriptDirectory() {
        try {
            Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath();
            Path kubeJSScriptsPath = gameDirectory.resolve("kubejs").resolve("server_scripts");
            Files.createDirectories(kubeJSScriptsPath);
            return kubeJSScriptsPath.resolve("oei_remove_recipe.js");
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to create kubejs/server_scripts directory", e);
            return null;
        }
    }

    private static List<String> collectRecipeIds(List<String> matchItems, List<String> matchTags) {
        List<String> recipeIds = new ArrayList<>();
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        int totalRecipes = 0;
        int checkedRecipes = 0;

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            totalRecipes++;
            ResourceLocation recipeId = recipe.getId();
            boolean shouldRemove = false;

            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : ingredient.getItems()) {
                    String itemId = Utils.getItemRegistryName(stack.getItem());
                    if (itemId != null && matchItems.contains(itemId)) {
                        shouldRemove = true;
                        break;
                    }
                }
                if (shouldRemove) break;
            }

            if (!shouldRemove) {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    RegistryAccess registryAccess = level.registryAccess();
                    ItemStack result = recipe.getResultItem(registryAccess);
                    if (!result.isEmpty()) {
                        String resultItemId = Utils.getItemRegistryName(result.getItem());
                        if (resultItemId != null && matchItems.contains(resultItemId)) {
                            Oneenoughitem.LOGGER.info("Found matching output item '{}' in recipe '{}'", resultItemId, recipeId);
                            shouldRemove = true;
                        }
                    }
                }
            }

            if (shouldRemove) {
                recipeIds.add(recipeId.toString());
            }

            checkedRecipes++;
        }

        return recipeIds;
    }

    private static String generateOptimizedScript(List<String> newRecipeIds, int itemCount, int tagCount, String fileName) {
        StringBuilder scriptContent = new StringBuilder();
        Set<String> allRecipeIds = new HashSet<>(newRecipeIds);

        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath();
        Path existingScriptFile = gameDirectory.resolve("kubejs").resolve("server_scripts").resolve("oei_remove_recipe.js");

        if (Files.exists(existingScriptFile)) {
            try {
                String existingContent = Files.readString(existingScriptFile);
                Set<String> existingRecipeIds = extractRecipeIdsFromScript(existingContent);
                allRecipeIds.addAll(existingRecipeIds);
                Oneenoughitem.LOGGER.info("Merged {} existing recipe IDs with {} new recipe IDs", existingRecipeIds.size(), newRecipeIds.size());
            } catch (Exception e) {
                Oneenoughitem.LOGGER.warn("Failed to read existing script file, will create new one: {}", e.getMessage());
            }
        }

        List<String> sortedRecipeIds = new ArrayList<>(allRecipeIds);
        sortedRecipeIds.sort(String::compareTo);

        scriptContent.append("// OEI自动生成的配方移除脚本\n");
        scriptContent.append("// 此脚本移除与被替换物品相关的配方\n");
        scriptContent.append("// 最后更新时间: ").append(LocalDateTime.now()).append("\n");
        scriptContent.append("// 最后更新源文件: ").append(fileName).append("\n");
        scriptContent.append("// 当前批次替换物品数量: ").append(itemCount).append("\n");
        scriptContent.append("// 当前批次替换标签数量: ").append(tagCount).append("\n");
        scriptContent.append("// 当前批次新增配方数量: ").append(newRecipeIds.size()).append("\n");
        scriptContent.append("// 总移除配方数量: ").append(sortedRecipeIds.size()).append("\n\n");

        scriptContent.append("ServerEvents.recipes(event => {\n");
        scriptContent.append("    // 需要移除的配方ID列表（已合并所有批次）\n");
        scriptContent.append("    const recipeIds = [\n");

        for (int i = 0; i < sortedRecipeIds.size(); i++) {
            scriptContent.append("        \"").append(sortedRecipeIds.get(i)).append("\"");
            if (i < sortedRecipeIds.size() - 1) {
                scriptContent.append(",");
            }
            scriptContent.append("\n");
        }

        scriptContent.append("    ];\n\n");
        scriptContent.append("    // 批量移除配方\n");
        scriptContent.append("    recipeIds.forEach(id => {\n");
        scriptContent.append("        event.remove({ id: id });\n");
        scriptContent.append("    });\n\n");
        scriptContent.append("    console.info(`OEI: 已移除 ${recipeIds.length} 个配方 (最后更新来源: ").append(fileName).append(")`);\n");
        scriptContent.append("});\n");

        return scriptContent.toString();
    }

    private static Set<String> extractRecipeIdsFromScript(String scriptContent) {
        Set<String> recipeIds = new HashSet<>();
        try {
            String[] lines = scriptContent.split("\n");
            boolean inRecipeArray = false;

            for (String line : lines) {
                line = line.trim();
                if (line.contains("const recipeIds = [")) {
                    inRecipeArray = true;
                    continue;
                }
                if (inRecipeArray) {
                    if (line.equals("];")) {
                        break;
                    }
                    if (line.startsWith("\"") && line.contains("\"")) {
                        String recipeId = line.substring(1);
                        int endIndex = recipeId.indexOf("\"");
                        if (endIndex > 0) {
                            recipeId = recipeId.substring(0, endIndex);
                            recipeIds.add(recipeId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.warn("Failed to extract recipe IDs from existing script: {}", e.getMessage());
        }
        return recipeIds;
    }
}