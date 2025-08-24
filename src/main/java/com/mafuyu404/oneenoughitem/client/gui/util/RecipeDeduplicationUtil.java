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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RecipeDeduplicationUtil {

    private static volatile boolean isDeduplicationInProgress = false;
    private static Consumer<Component> currentMessageCallback = null;

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

    /**
     * 配方去重方法 - 根据脚本文件存在情况决定执行流程
     *
     * @param targetFilePath  目标文件路径（可以为null，会扫描所有文件）
     * @param messageCallback 消息回调
     * @return 去重结果
     */
    public static DeduplicationResult deduplicateRecipes(Path targetFilePath, Consumer<Component> messageCallback) {
        if (!CompatUtil.isKubeJSLoaded()) {
            return DeduplicationResult.error("error.oneenoughitem.kubejs_not_loaded");
        }

        if (isDeduplicationInProgress) {
            return DeduplicationResult.warning("message.oneenoughitem.deduplication_in_progress");
        }

        try {
            isDeduplicationInProgress = true;
            currentMessageCallback = messageCallback;

            Path scriptFile = createScriptDirectory();
            if (scriptFile == null) {
                isDeduplicationInProgress = false;
                return DeduplicationResult.error("error.oneenoughitem.script_directory_creation_failed");
            }

            // 检查脚本文件是否存在，决定执行流程
            if (!Files.exists(scriptFile)) {
                // 没有脚本文件：直接基于当前数据生成脚本
                Oneenoughitem.LOGGER.info("No existing script found, generating script directly from current data");
                return generateScriptDirectly(scriptFile, messageCallback);
            } else {
                // 有脚本文件：执行三步流程（删除 → reload → 重新生成）
                Oneenoughitem.LOGGER.info("Existing script found, executing three-step process");
                return executeThreeStepProcess(scriptFile, messageCallback);
            }

        } catch (Exception e) {
            isDeduplicationInProgress = false;
            currentMessageCallback = null;
            Oneenoughitem.LOGGER.error("Failed to start recipe deduplication process", e);
            return DeduplicationResult.error("error.oneenoughitem.script_generation_failed");
        }
    }

    /**
     * 直接生成脚本（没有现有脚本时）
     */
    private static DeduplicationResult generateScriptDirectly(Path scriptFile, Consumer<Component> messageCallback) {
        try {
            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.generating_script_directly")
                        .withStyle(ChatFormatting.YELLOW));
            }

            Set<String> allMatchItems = new HashSet<>();
            Set<String> allMatchTags = new HashSet<>();

            // 扫描所有数据包中的替换规则文件
            collectMatchItemsFromAllFiles(allMatchItems, allMatchTags);

            if (allMatchItems.isEmpty() && allMatchTags.isEmpty()) {
                isDeduplicationInProgress = false;
                return DeduplicationResult.warning("warning.oneenoughitem.no_match_items");
            }

            // 基于当前配方数据收集需要移除的配方ID
            List<String> recipeIds = collectRecipeIds(new ArrayList<>(allMatchItems), new ArrayList<>(allMatchTags));

            if (recipeIds.isEmpty()) {
                isDeduplicationInProgress = false;
                return DeduplicationResult.warning("warning.oneenoughitem.no_recipes_found");
            }

            // 生成脚本
            String scriptContent = generateCompleteScript(recipeIds, allMatchItems.size(), allMatchTags.size());
            Files.writeString(scriptFile, scriptContent);

            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.script_generated_with_count", recipeIds.size())
                        .withStyle(ChatFormatting.GREEN));
                messageCallback.accept(Component.translatable("message.oneenoughitem.reload_required")
                        .withStyle(ChatFormatting.YELLOW));
            }

            Oneenoughitem.LOGGER.info("Generated KubeJS script directly with {} recipe removals at: {}",
                    recipeIds.size(), scriptFile);

            isDeduplicationInProgress = false;
            return DeduplicationResult.success("message.oneenoughitem.script_generated", recipeIds.size(), scriptFile);

        } catch (Exception e) {
            isDeduplicationInProgress = false;
            Oneenoughitem.LOGGER.error("Failed to generate script directly", e);
            return DeduplicationResult.error("error.oneenoughitem.script_generation_failed");
        }
    }

    /**
     * 执行三步流程（有现有脚本时）
     */
    private static DeduplicationResult executeThreeStepProcess(Path scriptFile, Consumer<Component> messageCallback) {
        try {
            // 步骤1: 删除现有脚本
            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.deduplication_step1")
                        .withStyle(ChatFormatting.YELLOW));
            }

            Files.delete(scriptFile);
            Oneenoughitem.LOGGER.info("Deleted existing script file: {}", scriptFile);
            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.script_deleted")
                        .withStyle(ChatFormatting.GREEN));
            }

            // 步骤2: 执行reload命令
            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.deduplication_step2")
                        .withStyle(ChatFormatting.YELLOW));
            }

            executeReloadCommand();

            // 步骤3: 延迟执行脚本生成（等待reload完成）
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                try {
                    if (messageCallback != null) {
                        messageCallback.accept(Component.translatable("message.oneenoughitem.deduplication_step3")
                                .withStyle(ChatFormatting.YELLOW));
                    }

                    generateScriptAfterReload(scriptFile, messageCallback);
                } finally {
                    isDeduplicationInProgress = false;
                    currentMessageCallback = null;
                }
            });

            return DeduplicationResult.success("message.oneenoughitem.deduplication_in_progress", 0, scriptFile);

        } catch (Exception e) {
            isDeduplicationInProgress = false;
            currentMessageCallback = null;
            Oneenoughitem.LOGGER.error("Failed to execute three-step process", e);
            return DeduplicationResult.error("error.oneenoughitem.script_generation_failed");
        }
    }

    /**
     * 执行reload命令
     */
    private static void executeReloadCommand() {
        try {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand("reload");
                Oneenoughitem.LOGGER.info("Executed reload command for recipe deduplication");
            }
        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to execute reload command", e);
            throw new RuntimeException("Failed to execute reload command", e);
        }
    }

    /**
     * 在reload完成后生成脚本
     */
    private static void generateScriptAfterReload(Path scriptFile, Consumer<Component> messageCallback) {
        try {
            Set<String> allMatchItems = new HashSet<>();
            Set<String> allMatchTags = new HashSet<>();

            // 扫描所有数据包中的替换规则文件
            collectMatchItemsFromAllFiles(allMatchItems, allMatchTags);

            if (allMatchItems.isEmpty() && allMatchTags.isEmpty()) {
                if (messageCallback != null) {
                    messageCallback.accept(Component.translatable("warning.oneenoughitem.no_match_items")
                            .withStyle(ChatFormatting.YELLOW));
                }
                return;
            }

            // 现在配方数据已经恢复，可以正确读取所有配方（为什么读不到！！！）
            List<String> recipeIds = collectRecipeIds(new ArrayList<>(allMatchItems), new ArrayList<>(allMatchTags));

            if (recipeIds.isEmpty()) {
                if (messageCallback != null) {
                    messageCallback.accept(Component.translatable("warning.oneenoughitem.no_recipes_found")
                            .withStyle(ChatFormatting.YELLOW));
                }
                return;
            }

            // 生成新的脚本
            String scriptContent = generateCompleteScript(recipeIds, allMatchItems.size(), allMatchTags.size());

            Files.writeString(scriptFile, scriptContent);

            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("message.oneenoughitem.deduplication_complete", recipeIds.size())
                        .withStyle(ChatFormatting.GREEN));
                messageCallback.accept(Component.translatable("message.oneenoughitem.reload_required_again")
                        .withStyle(ChatFormatting.YELLOW));
            }

            Oneenoughitem.LOGGER.info("Successfully generated KubeJS script with {} recipe removals after reload at: {}",
                    recipeIds.size(), scriptFile);

        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Failed to generate script after reload", e);
            if (messageCallback != null) {
                messageCallback.accept(Component.translatable("error.oneenoughitem.script_generation_failed")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    /**
     * 从所有数据包文件中收集匹配项
     */
    private static void collectMatchItemsFromAllFiles(Set<String> allMatchItems, Set<String> allMatchTags) {
        List<PathUtils.FileInfo> allFiles = PathUtils.scanAllReplacementFiles();

        Oneenoughitem.LOGGER.info("Found {} replacement files to scan", allFiles.size());

        for (PathUtils.FileInfo fileInfo : allFiles) {
            int itemsBefore = allMatchItems.size();
            int tagsBefore = allMatchTags.size();

            collectMatchItemsFromFile(fileInfo.filePath(), allMatchItems, allMatchTags);

            int itemsAdded = allMatchItems.size() - itemsBefore;
            int tagsAdded = allMatchTags.size() - tagsBefore;

            if (itemsAdded > 0 || tagsAdded > 0) {
                Oneenoughitem.LOGGER.info("File '{}' contributed {} items and {} tags",
                        fileInfo.displayName(), itemsAdded, tagsAdded);
            }
        }

        Oneenoughitem.LOGGER.info("Total collected from all files: {} unique items and {} unique tags",
                allMatchItems.size(), allMatchTags.size());
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
                                Oneenoughitem.LOGGER.debug("Expanded tag {} to {} items", tagId, tagItems.size());
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

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            totalRecipes++;
            ResourceLocation recipeId = recipe.getId();
            boolean shouldRemove = false;

            // 检查配方输入材料
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

            // 检查配方输出物品
            if (!shouldRemove) {
                Level level = Minecraft.getInstance().level;
                if (level != null) {
                    RegistryAccess registryAccess = level.registryAccess();
                    ItemStack result = recipe.getResultItem(registryAccess);
                    if (!result.isEmpty()) {
                        String resultItemId = Utils.getItemRegistryName(result.getItem());
                        if (resultItemId != null && matchItems.contains(resultItemId)) {
                            Oneenoughitem.LOGGER.debug("Found matching output item '{}' in recipe '{}'", resultItemId, recipeId);
                            shouldRemove = true;
                        }
                    }
                }
            }

            if (shouldRemove) {
                recipeIds.add(recipeId.toString());
            }
        }

        Oneenoughitem.LOGGER.info("Checked {} recipes, found {} recipes to remove", totalRecipes, recipeIds.size());
        return recipeIds;
    }

    /**
     * 生成完全覆盖的脚本
     */
    private static String generateCompleteScript(List<String> recipeIds, int itemCount, int tagCount) {
        StringBuilder scriptContent = new StringBuilder();

        List<String> sortedRecipeIds = new ArrayList<>(recipeIds);
        sortedRecipeIds.sort(String::compareTo);

        scriptContent.append("// OEI自动生成的配方移除脚本\n");
        scriptContent.append("// 此脚本移除与被替换物品相关的配方\n");
        scriptContent.append("// 生成时间: ").append(LocalDateTime.now()).append("\n");
        scriptContent.append("// 扫描来源: 所有数据包中的替换规则文件\n");
        scriptContent.append("// 替换物品数量: ").append(itemCount).append("\n");
        scriptContent.append("// 替换标签数量: ").append(tagCount).append("\n");
        scriptContent.append("// 移除配方数量: ").append(sortedRecipeIds.size()).append("\n");
        scriptContent.append("// 注意: 此脚本通过三步流程生成（删除->reload->重新生成）\n\n");

        scriptContent.append("ServerEvents.recipes(event => {\n");
        scriptContent.append("    // 需要移除的配方ID列表（基于reload后的完整配方数据）\n");
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
        scriptContent.append("    console.debug(`OEI: 已移除 ${recipeIds.length} 个配方 (基于reload后的完整数据)`);\n");
        scriptContent.append("});\n");

        return scriptContent.toString();
    }
}