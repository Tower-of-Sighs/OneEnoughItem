package com.mafuyu404.oneenoughitem.client.gui.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.cache.GlobalReplacementCache;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.ReplacementCache;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ReplacementEditorManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<Item> matchItems = new HashSet<>();
    private final Set<ResourceLocation> matchTags = new HashSet<>();
    private Item resultItem;
    private ResourceLocation resultTag;

    private Path currentFilePath;
    private String currentFileName = "";

    private int currentObjectIndex = -1; // -1 表示添加模式
    private JsonArray currentJsonObjects;

    private Runnable uiUpdateCallback;

    // Getters
    public Set<Item> getMatchItems() {
        return new HashSet<>(matchItems);
    }

    public Set<ResourceLocation> getMatchTags() {
        return new HashSet<>(matchTags);
    }

    public Item getResultItem() {
        return resultItem;
    }

    public ResourceLocation getResultTag() {
        return resultTag;
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }

    public int getCurrentObjectIndex() {
        return currentObjectIndex;
    }

    public int getObjectSize() {
        return currentJsonObjects != null ? currentJsonObjects.size() : 0;
    }

    public void setUiUpdateCallback(Runnable callback) {
        this.uiUpdateCallback = callback;
    }

    private void notifyUiUpdate() {
        if (this.uiUpdateCallback != null) {
            this.uiUpdateCallback.run();
        }
    }

    public void addMatchItem(Item item) {
        this.matchItems.add(item);
    }

    public void addMatchTag(ResourceLocation tagId) {
        this.matchTags.add(tagId);
    }

    public void removeMatchTag(ResourceLocation tagId) {
        this.matchTags.remove(tagId);
    }

    public boolean removeMatchItem(Item item) {
        String targetItemId = Utils.getItemRegistryName(item);
        if (targetItemId == null) {
            Oneenoughitem.LOGGER.warn("removeMatchItem: Target item ID is null for item: {}", item);
            return false;
        }

        return this.matchItems.removeIf(existingItem -> {
            String existingItemId = Utils.getItemRegistryName(existingItem);
            return targetItemId.equals(existingItemId);
        });
    }

    public void setResultItem(Item item) {
        this.resultItem = item;
        this.resultTag = null;
    }

    public void setResultTag(ResourceLocation tagId) {
        this.resultTag = tagId;
        this.resultItem = null;
    }

    public void clearMatchItems() {
        this.matchItems.clear();
        this.matchTags.clear();
    }

    public void clearResultItem() {
        this.resultItem = null;
        this.resultTag = null;
    }

    public void clearAll() {
        clearMatchItems();
        clearResultItem();
        this.currentFileName = "";
        this.currentFilePath = null;
        this.currentObjectIndex = -1;
        this.currentJsonObjects = null;
    }

    public void setCurrentObjectIndex(int index) {
        this.currentObjectIndex = index;
        if (index >= 0 && this.currentJsonObjects != null && index < this.currentJsonObjects.size()) {
            loadReplacementFromObject(index);
        } else {
            clearMatchItems();
            clearResultItem();
            notifyUiUpdate();
        }
    }

    private void loadReplacementFromObject(int index) {
        executeWithErrorHandling(() -> {
            JsonElement element = this.currentJsonObjects.get(index);
            var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);

            if (result.result().isPresent()) {
                Replacements replacement = result.result().get();

                clearMatchItems();
                clearResultItem();

                // 处理匹配项
                for (String matchItem : replacement.matchItems()) {
                    if (matchItem.startsWith("#")) {
                        this.matchTags.add(new ResourceLocation(matchItem.substring(1)));
                    } else {
                        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(matchItem));
                        if (item != null && item != Items.AIR) {
                            this.matchItems.add(item);
                        }
                    }
                }

                // 处理结果项
                String resultString = replacement.resultItems();
                if (resultString.startsWith("#")) {
                    this.resultTag = new ResourceLocation(resultString.substring(1));
                } else {
                    this.resultItem = BuiltInRegistries.ITEM.get(new ResourceLocation(resultString));
                }

                notifyUiUpdate();
                showMessage(Component.translatable("message.oneenoughitem.object_loaded", index + 1)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                showError(Component.translatable("error.oneenoughitem.object_parse_failed", index + 1)
                        .withStyle(ChatFormatting.RED));
            }
        }, e -> showError(Component.translatable("error.oneenoughitem.object_load_error", e.getMessage())
                .withStyle(ChatFormatting.RED)));
    }

    public void deleteObjectElement(int index) {
        if (this.currentJsonObjects == null || index < 0 || index >= this.currentJsonObjects.size()) {
            showError(Component.translatable("error.oneenoughitem.object_index_invalid")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        executeWithErrorHandling(() -> {
            // 从全局缓存中移除对应的替换规则
            JsonElement element = this.currentJsonObjects.get(index);
            removeReplacementFromCache(element);

            // 删除JSON元素
            this.currentJsonObjects.remove(index);
            this.saveJsonObjectsToFile(this.currentJsonObjects);

            showMessage(Component.translatable("message.oneenoughitem.object_element_deleted", index + 1)
                    .withStyle(ChatFormatting.GREEN));

            // 如果删除后数组为空，重建全局缓存以确保一致性
            if (this.currentJsonObjects.isEmpty()) {
                Oneenoughitem.LOGGER.debug("JSON array is now empty, rebuilding global cache to ensure consistency");
                GlobalReplacementCache.rebuild();
            }
        }, e -> showError(Component.translatable("error.oneenoughitem.object_delete_failed", e.getMessage())
                .withStyle(ChatFormatting.RED)));
    }

    private void removeReplacementFromCache(JsonElement element) {
        var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);
        if (result.result().isPresent()) {
            Replacements replacement = result.result().get();

            List<String> matchItemsList = new ArrayList<>();
            List<String> matchTagsList = new ArrayList<>();

            for (String matchItem : replacement.matchItems()) {
                if (matchItem.startsWith("#")) {
                    matchTagsList.add(matchItem.substring(1));
                } else {
                    matchItemsList.add(matchItem);
                }
            }

            // 从全局缓存中移除
            GlobalReplacementCache.removeReplacement(matchItemsList, matchTagsList);

            // 从运行时缓存中移除
            ReplacementCache.removeReplacements(matchItemsList, matchTagsList);

            Oneenoughitem.LOGGER.debug("Removed replacement from both global and runtime cache: items={}, tags={}",
                    matchItemsList, matchTagsList);
        }
    }

    public void createReplacementFile(String datapackName, String fileName) {
        executeWithErrorHandling(() -> {
            Path datapackPath = PathUtils.getDatapackPath(datapackName);
            Path replacementsPath = datapackPath.resolve("data/oei/replacements");

            Files.createDirectories(replacementsPath);
            createPackMcmetaIfNeeded(datapackPath);

            Path filePath = replacementsPath.resolve(fileName + ".json");

            // 检查文件是否已存在
            if (Files.exists(filePath)) {
                showError(Component.translatable("error.oneenoughitem.file_already_exists", fileName + ".json")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            JsonArray emptyObjects = new JsonArray();

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(emptyObjects, writer);
            }

            this.currentFilePath = filePath;
            this.currentFileName = fileName;
            this.currentJsonObjects = emptyObjects;
            this.currentObjectIndex = -1;

            showMessage(Component.translatable("message.oneenoughitem.file_created", filePath.toString())
                    .withStyle(ChatFormatting.GREEN));
            Oneenoughitem.LOGGER.info("Created replacement file: {}", filePath);
        }, e -> showError(Component.translatable("error.oneenoughitem.file_create_failed", e.getMessage())
                .withStyle(ChatFormatting.RED)));
    }

    private void createPackMcmetaIfNeeded(Path datapackPath) throws IOException {
        Path packMcmetaPath = datapackPath.resolve("pack.mcmeta");
        if (!Files.exists(packMcmetaPath)) {
            String packMcmetaContent = """
                    {
                       "pack": {
                         "description": "The automatically generated OEI data pack",
                         "pack_format": 15
                       }
                    }""";

            try (FileWriter writer = new FileWriter(packMcmetaPath.toFile())) {
                writer.write(packMcmetaContent);
            }
            Oneenoughitem.LOGGER.info("Created pack.mcmeta file: {}", packMcmetaPath);
        }
    }

    public void selectJsonFile(Path selectedPath, int mode) {
        executeWithErrorHandling(() -> {
            this.currentFilePath = selectedPath;
            String fileName = selectedPath.getFileName().toString();
            this.currentFileName = fileName.endsWith(".json") ?
                    fileName.substring(0, fileName.length() - 5) : fileName;

            if (!Files.exists(this.currentFilePath)) {
                createEmptyJsonFile();
            } else {
                this.currentJsonObjects = this.readExistingJsonObjects();
            }

            clearMatchItems();
            clearResultItem();
            setupModeAndIndex(mode);
            notifyUiUpdate();

            String modeKey = getModeTranslationKey(mode);
            showMessage(Component.translatable("message.oneenoughitem.file_selected",
                    fileName, Component.translatable(modeKey)).withStyle(ChatFormatting.GREEN));
        }, e -> showError(Component.translatable("error.oneenoughitem.file_select_failed", e.getMessage())
                .withStyle(ChatFormatting.RED)));
    }

    private void createEmptyJsonFile() throws IOException {
        Files.createDirectories(this.currentFilePath.getParent());
        JsonArray emptyObjects = new JsonArray();
        try (FileWriter writer = new FileWriter(this.currentFilePath.toFile())) {
            GSON.toJson(emptyObjects, writer);
        }
        this.currentJsonObjects = emptyObjects;
    }

    private void setupModeAndIndex(int mode) {
        if (mode == 1) {
            this.currentObjectIndex = 0; // 默认选择第一个元素
            if (!this.currentJsonObjects.isEmpty()) {
                loadReplacementFromObject(0);
            } else {
                this.currentObjectIndex = -1; // 如果数组为空，保持添加模式
            }
        } else {
            this.currentObjectIndex = -1;
        }
    }

    private String getModeTranslationKey(int mode) {
        return switch (mode) {
            case 0 -> "mode.oneenoughitem.add";
            case 1 -> "mode.oneenoughitem.modify";
            case 2 -> "mode.oneenoughitem.remove";
            default -> "mode.oneenoughitem.unknown";
        };
    }

    public String getObjectElementDescription(int index) {
        if (this.currentJsonObjects == null || index < 0 || index >= this.currentJsonObjects.size()) {
            return Component.translatable("description.oneenoughitem.invalid").getString();
        }

        try {
            JsonElement element = this.currentJsonObjects.get(index);
            var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);

            if (result.result().isPresent()) {
                Replacements replacement = result.result().get();
                int matchCount = replacement.matchItems().size();
                String resultType = replacement.resultItems().startsWith("#") ?
                        Component.translatable("description.oneenoughitem.tag").getString() :
                        Component.translatable("description.oneenoughitem.item").getString();

                return Component.translatable("description.oneenoughitem.items_to_type", matchCount, resultType).getString();
            } else {
                return Component.translatable("description.oneenoughitem.parse_failed").getString();
            }
        } catch (Exception e) {
            return Component.translatable("description.oneenoughitem.error").getString();
        }
    }

    public void setCurrentFileName(String fileName) {
        this.currentFileName = fileName != null ? fileName : "";
    }

    public void saveReplacement() {
        // 验证输入
        ValidationResult validation = validateReplacementInput();
        if (!validation.isValid()) {
            showError(validation.errorMessage());
            return;
        }

        if (this.currentFilePath == null) {
            showError(Component.translatable("error.oneenoughitem.no_file_selected")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        executeWithErrorHandling(() -> {
            // 准备数据
            ReplacementData data = prepareReplacementData();

            // 添加到全局缓存
            GlobalReplacementCache.addReplacement(
                    data.matchItemsList(),
                    data.matchTagsList(),
                    data.resultItemString().startsWith("#") ? null : data.resultItemString(),
                    data.resultItemString().startsWith("#") ? data.resultItemString() : null,
                    this.currentFileName
            );

            // 合并匹配项和标签到一个列表中
            List<String> allMatchItems = new ArrayList<>(data.matchItemsList());
            // 为标签添加 # 前缀
            for (String tag : data.matchTagsList()) {
                allMatchItems.add("#" + tag);
            }

            // 创建替换对象并编码
            Replacements replacement = new Replacements(allMatchItems, data.resultItemString());
            var result = Replacements.CODEC.encodeStart(JsonOps.INSTANCE, replacement);

            if (result.result().isEmpty()) {
                showError(Component.translatable("error.oneenoughitem.encode_failed")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            JsonElement replacementElement = result.result().get();

            // 保存到JSON
            if (this.currentObjectIndex >= 0) {
                updateExistingObject(replacementElement);
            } else {
                addNewObject(replacementElement);
            }

            this.saveJsonObjectsToFile(this.currentJsonObjects);
            Oneenoughitem.LOGGER.info("Saved replacement to file: {}", this.currentFilePath);

        }, e -> {
            if (e instanceof IOException) {
                showError(Component.translatable("error.oneenoughitem.save_failed", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            } else {
                showError(Component.translatable("error.oneenoughitem.unexpected_error", e.getMessage())
                        .withStyle(ChatFormatting.RED));
            }
        });
    }

    private record ValidationResult(boolean isValid, Component errorMessage) {
    }

    private ValidationResult validateReplacementInput() {
        boolean hasMatchItems = !this.matchItems.isEmpty() || !this.matchTags.isEmpty();
        boolean hasResultItem = this.resultItem != null || this.resultTag != null;

        if (!hasMatchItems && !hasResultItem) {
            return new ValidationResult(false,
                    Component.translatable("error.oneenoughitem.both_empty").withStyle(ChatFormatting.RED));
        } else if (!hasMatchItems) {
            return new ValidationResult(false,
                    Component.translatable("error.oneenoughitem.missing_match_items").withStyle(ChatFormatting.RED));
        } else if (!hasResultItem) {
            return new ValidationResult(false,
                    Component.translatable("error.oneenoughitem.missing_result_item").withStyle(ChatFormatting.RED));
        }

        return new ValidationResult(true, null);
    }

    private record ReplacementData(List<String> matchItemsList, List<String> matchTagsList, String resultItemString) {
    }

    private ReplacementData prepareReplacementData() {
        List<String> matchItemsList = new ArrayList<>();
        List<String> matchTagsList = new ArrayList<>();

        for (Item item : this.matchItems) {
            String itemId = Utils.getItemRegistryName(item);
            if (itemId != null) {
                matchItemsList.add(itemId);
            }
        }

        for (ResourceLocation tagId : this.matchTags) {
            matchTagsList.add(tagId.toString());
        }

        String resultItemString = getResultItemString();
        if (resultItemString == null) {
            throw new RuntimeException("Failed to get result item string");
        }

        return new ReplacementData(matchItemsList, matchTagsList, resultItemString);
    }

    private void updateExistingObject(JsonElement replacementElement) {
        if (this.currentObjectIndex < this.currentJsonObjects.size()) {
            this.currentJsonObjects.set(this.currentObjectIndex, replacementElement);
            showMessage(Component.translatable("message.oneenoughitem.object_updated", this.currentObjectIndex + 1)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            showError(Component.translatable("error.oneenoughitem.object_index_out_of_bounds")
                    .withStyle(ChatFormatting.RED));
            throw new RuntimeException("Object index out of bounds");
        }
    }

    private void addNewObject(JsonElement replacementElement) {
        this.currentJsonObjects.add(replacementElement);
        showMessage(Component.translatable("message.oneenoughitem.replacement_added",
                this.currentFilePath.getFileName().toString()).withStyle(ChatFormatting.GREEN));
    }

    public void deleteFile(Path filePath) {
        executeWithErrorHandling(() -> {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                showMessage(Component.translatable("message.oneenoughitem.file_deleted",
                        filePath.getFileName().toString()).withStyle(ChatFormatting.GREEN));

                if (filePath.equals(this.currentFilePath)) {
                    resetCurrentFile();
                }
            } else {
                showError(Component.translatable("error.oneenoughitem.file_not_exists", filePath.toString())
                        .withStyle(ChatFormatting.RED));
            }
        }, e -> showError(Component.translatable("error.oneenoughitem.file_delete_failed", e.getMessage())
                .withStyle(ChatFormatting.RED)));
    }

    private void resetCurrentFile() {
        this.currentFilePath = null;
        this.currentFileName = "";
        this.currentJsonObjects = null;
        this.currentObjectIndex = -1;
        clearMatchItems();
        clearResultItem();
        notifyUiUpdate();
    }

    private String getResultItemString() {
        if (this.resultItem != null) {
            String resultItemString = Utils.getItemRegistryName(this.resultItem);
            if (resultItemString == null) {
                showError(Component.translatable("error.oneenoughitem.result_item_name_failed")
                        .withStyle(ChatFormatting.RED));
                return null;
            }
            return resultItemString;
        } else if (this.resultTag != null) {
            return "#" + this.resultTag;
        } else {
            showError(Component.translatable("error.oneenoughitem.no_result_item")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
    }

    private void executeWithErrorHandling(RunnableWithException operation, Consumer<Exception> errorHandler) {
        try {
            operation.run();
        } catch (Exception e) {
            Oneenoughitem.LOGGER.error("Operation failed", e);
            errorHandler.accept(e);
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }

    private void showMessage(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }

    private void showError(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }

    private JsonArray readExistingJsonObjects() {
        JsonArray existingObjects;
        if (Files.exists(this.currentFilePath)) {
            try {
                String content = Files.readString(this.currentFilePath);
                if (content.trim().isEmpty()) {
                    existingObjects = new JsonArray();
                } else {
                    JsonElement parsed = GSON.fromJson(content, JsonElement.class);
                    if (parsed != null && parsed.isJsonArray()) {
                        existingObjects = parsed.getAsJsonArray();
                    } else {
                        this.showError(Component.translatable("error.oneenoughitem.file_format_error").withStyle(ChatFormatting.RED));
                        existingObjects = new JsonArray();
                    }
                }
            } catch (Exception e) {
                this.showError(Component.translatable("error.oneenoughitem.file_parse_failed", e.getMessage()).withStyle(ChatFormatting.RED));
                Oneenoughitem.LOGGER.warn("Failed to parse existing file, creating new objects", e);
                existingObjects = new JsonArray();
            }
        } else {
            existingObjects = new JsonArray();
        }
        return existingObjects;
    }

    private void saveJsonObjectsToFile(JsonArray jsonObjects) throws IOException {
        try (FileWriter writer = new FileWriter(this.currentFilePath.toFile())) {
            GSON.toJson(jsonObjects, writer);
            writer.flush();
        }
    }

    public void reloadDatapacks() {
        try {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand("reload");

                GlobalReplacementCache.rebuild();

                this.showMessage(Component.translatable("message.oneenoughitem.datapack_reload_triggered").withStyle(ChatFormatting.GREEN));
                Oneenoughitem.LOGGER.info("Datapacks reloaded and global cache rebuilt");
            }
        } catch (Exception e) {
            this.showError(Component.translatable("error.oneenoughitem.reload_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to reload datapacks", e);
        }
    }
}