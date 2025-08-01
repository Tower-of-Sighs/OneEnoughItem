package com.mafuyu404.oneenoughitem.client.gui.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import com.mafuyu404.oneenoughitem.data.Replacements;
import com.mafuyu404.oneenoughitem.init.Utils;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ReplacementEditorManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<Item> matchItems = new HashSet<>();
    private final Set<ResourceLocation> matchTags = new HashSet<>();
    private Item resultItem;
    private ResourceLocation resultTag;

    private Path currentFilePath;
    private String currentFileName = "";

    private int currentArrayIndex = -1; // -1 表示添加模式
    private JsonArray currentJsonArray;

    private Runnable uiUpdateCallback;

    public Set<Item> getMatchItems() { return new HashSet<>(matchItems); }
    public Set<ResourceLocation> getMatchTags() { return new HashSet<>(matchTags); }
    public Item getResultItem() { return resultItem; }
    public ResourceLocation getResultTag() { return resultTag; }
    public String getCurrentFileName() { return currentFileName; }
    public int getCurrentArrayIndex() { return currentArrayIndex; }
    public int getArraySize() { return currentJsonArray != null ? currentJsonArray.size() : 0; }

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

    public void removeMatchItem(Item item) {
        this.matchItems.remove(item);
    }

    public void removeMatchTag(ResourceLocation tagId) {
        this.matchTags.remove(tagId);
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
        this.currentArrayIndex = -1;
        this.currentJsonArray = null;
    }

    public void setCurrentArrayIndex(int index) {
        this.currentArrayIndex = index;
        if (index >= 0 && this.currentJsonArray != null && index < this.currentJsonArray.size()) {
            loadReplacementFromArray(index);
        } else {
            clearMatchItems();
            clearResultItem();
            notifyUiUpdate();
        }
    }

    private void loadReplacementFromArray(int index) {
        try {
            JsonElement element = this.currentJsonArray.get(index);
            var result = Replacements.CODEC.parse(JsonOps.INSTANCE, element);

            if (result.result().isPresent()) {
                Replacements replacement = result.result().get();

                clearMatchItems();
                clearResultItem();

                Set<String> processedItems = new LinkedHashSet<>();

                for (String matchItem : replacement.matchItems()) {
                    if (processedItems.contains(matchItem)) {
                        continue;
                    }
                    processedItems.add(matchItem);

                    if (matchItem.startsWith("#")) {
                        ResourceLocation tagId = ResourceLocation.parse(matchItem.substring(1));
                        this.matchTags.add(tagId);
                    } else {
                        ResourceLocation itemId = ResourceLocation.parse(matchItem);
                        Item item = BuiltInRegistries.ITEM.get(itemId);
                        this.matchItems.add(item);
                    }
                }

                String resultString = replacement.resultItems();
                if (resultString.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.parse(resultString.substring(1));
                    this.resultTag = tagId;
                } else {
                    ResourceLocation itemId = ResourceLocation.parse(resultString);
                    this.resultItem = BuiltInRegistries.ITEM.get(itemId);
                }

                notifyUiUpdate();
                this.showMessage(Component.translatable("message.oneenoughitem.array_loaded", index + 1).withStyle(ChatFormatting.GREEN));
            } else {
                this.showError(Component.translatable("error.oneenoughitem.array_parse_failed", index + 1).withStyle(ChatFormatting.RED));
            }
        } catch (Exception e) {
            this.showError(Component.translatable("error.oneenoughitem.array_load_error", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to load replacement from array", e);
        }
    }

    public void deleteArrayElement(int index) {
        if (this.currentJsonArray == null || index < 0 || index >= this.currentJsonArray.size()) {
            this.showError(Component.translatable("error.oneenoughitem.array_index_invalid").withStyle(ChatFormatting.RED));
            return;
        }

        try {
            this.currentJsonArray.remove(index);
            this.saveJsonArrayToFile(this.currentJsonArray);

            // 如果删除的是当前编辑的元素，切换到添加模式
            if (this.currentArrayIndex == index) {
                setCurrentArrayIndex(-1);
            } else if (this.currentArrayIndex > index) {
                // 如果当前索引在删除索引之后，需要调整
                this.currentArrayIndex--;
            }

            this.showMessage(Component.translatable("message.oneenoughitem.array_element_deleted", index + 1).withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            this.showError(Component.translatable("error.oneenoughitem.array_delete_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to delete array element", e);
        }
    }

    public void createReplacementFile(String datapackName, String fileName) {
        try {
            Path datapackPath = PathUtils.getDatapackPath(datapackName);
            Path replacementsPath = datapackPath.resolve("data/oneenoughitem/oneenoughitem/replacements");

            Files.createDirectories(replacementsPath);

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

            Path filePath = replacementsPath.resolve(fileName + ".json");
            JsonArray emptyArray = new JsonArray();

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(emptyArray, writer);
            }

            this.currentFilePath = filePath;
            this.currentFileName = fileName;
            this.currentJsonArray = emptyArray;
            this.currentArrayIndex = -1;

            this.showMessage(Component.translatable("message.oneenoughitem.file_created", filePath.toString()).withStyle(ChatFormatting.GREEN));
            Oneenoughitem.LOGGER.info("Created replacement file: {}", filePath);

        } catch (IOException e) {
            this.showError(Component.translatable("error.oneenoughitem.file_create_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to create replacement file", e);
        }
    }

    public void selectJsonFile(Path selectedPath, int mode) {
        try {
            this.currentFilePath = selectedPath;
            String fileName = selectedPath.getFileName().toString();
            if (fileName.endsWith(".json")) {
                this.currentFileName = fileName.substring(0, fileName.length() - 5);
            } else {
                this.currentFileName = fileName;
            }

            if (!Files.exists(this.currentFilePath)) {
                try {
                    Files.createDirectories(this.currentFilePath.getParent());
                    JsonArray emptyArray = new JsonArray();
                    try (FileWriter writer = new FileWriter(this.currentFilePath.toFile())) {
                        GSON.toJson(emptyArray, writer);
                    }
                    this.currentJsonArray = emptyArray;
                } catch (IOException e) {
                    this.showError(Component.translatable("error.oneenoughitem.file_create_failed", e.getMessage()).withStyle(ChatFormatting.RED));
                    return;
                }
            } else {
                this.currentJsonArray = this.readExistingJsonArray();
            }

            clearMatchItems();
            clearResultItem();

            if (mode == 1) {
                this.currentArrayIndex = 0; // 默认选择第一个元素
                if (!this.currentJsonArray.isEmpty()) {
                    loadReplacementFromArray(0);
                } else {
                    // 如果数组为空，保持添加模式
                    this.currentArrayIndex = -1;
                }
            } else {
                this.currentArrayIndex = -1;
            }

            notifyUiUpdate();

            String modeKey = switch (mode) {
                case 0 -> "mode.oneenoughitem.add";
                case 1 -> "mode.oneenoughitem.modify";
                case 2 -> "mode.oneenoughitem.remove";
                default -> "mode.oneenoughitem.unknown";
            };

            this.showMessage(Component.translatable("message.oneenoughitem.file_selected",
                    fileName, Component.translatable(modeKey)).withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            this.showError(Component.translatable("error.oneenoughitem.file_select_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to select file", e);
        }
    }


    public String getArrayElementDescription(int index) {
        if (this.currentJsonArray == null || index < 0 || index >= this.currentJsonArray.size()) {
            return Component.translatable("description.oneenoughitem.invalid").getString();
        }

        try {
            JsonElement element = this.currentJsonArray.get(index);
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
        if ((this.matchItems.isEmpty() && this.matchTags.isEmpty()) &&
                (this.resultItem == null && this.resultTag == null)) {
            this.showError(Component.translatable("error.oneenoughitem.replacement_incomplete").withStyle(ChatFormatting.RED));
            return;
        }

        if (this.currentFilePath == null) {
            this.showError(Component.translatable("error.oneenoughitem.no_file_selected").withStyle(ChatFormatting.RED));
            return;
        }

        try {
            List<String> matchItemsList = new ArrayList<>();

            for (Item item : this.matchItems) {
                String itemId = Utils.getItemRegistryName(item);
                if (itemId != null) {
                    matchItemsList.add(itemId);
                } else {
                    Oneenoughitem.LOGGER.warn("Could not get registry name for item: {}", item);
                }
            }

            for (ResourceLocation tagId : this.matchTags) {
                matchItemsList.add("#" + tagId.toString());
            }

            String resultItemString = this.getResultItemString();
            if (resultItemString == null) {
                return;
            }

            Replacements replacement = new Replacements(matchItemsList, resultItemString);

            var result = Replacements.CODEC.encodeStart(JsonOps.INSTANCE, replacement);
            if (result.result().isEmpty()) {
                this.showError(Component.translatable("error.oneenoughitem.encode_failed").withStyle(ChatFormatting.RED));
                return;
            }

            JsonElement replacementElement = result.result().get();

            if (this.currentArrayIndex >= 0) {
                if (this.currentArrayIndex < this.currentJsonArray.size()) {
                    this.currentJsonArray.set(this.currentArrayIndex, replacementElement);
                    this.showMessage(Component.translatable("message.oneenoughitem.array_updated", this.currentArrayIndex + 1).withStyle(ChatFormatting.GREEN));
                } else {
                    this.showError(Component.translatable("error.oneenoughitem.array_index_out_of_bounds").withStyle(ChatFormatting.RED));
                    return;
                }
            } else {
                this.currentJsonArray.add(replacementElement);
                this.showMessage(Component.translatable("message.oneenoughitem.replacement_added", this.currentFilePath.getFileName().toString()).withStyle(ChatFormatting.GREEN));
            }

            this.saveJsonArrayToFile(this.currentJsonArray);
            Oneenoughitem.LOGGER.info("Saved replacement to file: {}", this.currentFilePath);

        } catch (IOException e) {
            this.showError(Component.translatable("error.oneenoughitem.save_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to save replacement", e);
        } catch (Exception e) {
            this.showError(Component.translatable("error.oneenoughitem.unexpected_error", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Unexpected error while saving replacement", e);
        }
    }

    private String getResultItemString() {
        if (this.resultItem != null) {
            String resultItemString = Utils.getItemRegistryName(this.resultItem);
            if (resultItemString == null) {
                this.showError(Component.translatable("error.oneenoughitem.result_item_name_failed").withStyle(ChatFormatting.RED));
                return null;
            }
            return resultItemString;
        } else if (this.resultTag != null) {
            return "#" + this.resultTag;
        } else {
            this.showError(Component.translatable("error.oneenoughitem.no_result_item").withStyle(ChatFormatting.RED));
            return null;
        }
    }

    private JsonArray readExistingJsonArray() {
        JsonArray existingArray;
        if (Files.exists(this.currentFilePath)) {
            try {
                String content = Files.readString(this.currentFilePath);
                if (content.trim().isEmpty()) {
                    existingArray = new JsonArray();
                } else {
                    JsonElement parsed = GSON.fromJson(content, JsonElement.class);
                    if (parsed != null && parsed.isJsonArray()) {
                        existingArray = parsed.getAsJsonArray();
                    } else {
                        this.showError(Component.translatable("error.oneenoughitem.file_format_error").withStyle(ChatFormatting.RED));
                        existingArray = new JsonArray();
                    }
                }
            } catch (Exception e) {
                this.showError(Component.translatable("error.oneenoughitem.file_parse_failed", e.getMessage()).withStyle(ChatFormatting.RED));
                Oneenoughitem.LOGGER.warn("Failed to parse existing file, creating new array", e);
                existingArray = new JsonArray();
            }
        } else {
            existingArray = new JsonArray();
        }
        return existingArray;
    }

    private void saveJsonArrayToFile(JsonArray jsonArray) throws IOException {
        try (FileWriter writer = new FileWriter(this.currentFilePath.toFile())) {
            GSON.toJson(jsonArray, writer);
            writer.flush();
        }
    }

    public void loadFromFile(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                this.showError(Component.translatable("error.oneenoughitem.file_not_exists", filePath.toString()).withStyle(ChatFormatting.RED));
                return;
            }

            this.currentFilePath = filePath;
            String fileName = filePath.getFileName().toString();
            if (fileName.endsWith(".json")) {
                this.currentFileName = fileName.substring(0, fileName.length() - 5);
            } else {
                this.currentFileName = fileName;
            }

            this.currentJsonArray = this.readExistingJsonArray();
            this.currentArrayIndex = -1;

            this.showMessage(Component.translatable("message.oneenoughitem.file_loaded", fileName).withStyle(ChatFormatting.GREEN));

        } catch (Exception e) {
            this.showError(Component.translatable("error.oneenoughitem.file_load_failed", e.getMessage()).withStyle(ChatFormatting.RED));
            Oneenoughitem.LOGGER.error("Failed to load file", e);
        }
    }

    public void reloadDatapacks() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendCommand("reload");
            this.showMessage(Component.translatable("message.oneenoughitem.datapack_reload_triggered").withStyle(ChatFormatting.GREEN));
        }
    }

    private void showMessage(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }

    private void showError(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }
}