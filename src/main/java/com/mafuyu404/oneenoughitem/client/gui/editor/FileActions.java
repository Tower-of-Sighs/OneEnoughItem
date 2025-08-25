package com.mafuyu404.oneenoughitem.client.gui.editor;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import com.mafuyu404.oneenoughitem.client.gui.FileSelectionScreen;
import com.mafuyu404.oneenoughitem.client.gui.ReplacementEditorScreen;
import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileActions {
    private final ReplacementEditorManager manager;

    public FileActions(ReplacementEditorManager manager) {
        this.manager = manager;
    }

    public String createFile(String datapackName, String fileName) {
        this.manager.createReplacementFile(datapackName, fileName);
        return this.manager.getCurrentFileName();
    }

    public void saveToJson() {
        this.manager.saveReplacement();
    }

    public void selectFile(Minecraft mc, ReplacementEditorScreen parent) {
        if (mc != null) {
            mc.setScreen(new FileSelectionScreen(parent));
        }
    }

    public List<Path> scanReplacementFiles() {
        List<Path> jsonFiles = new ArrayList<>();
        try {
            Path replacementsPath = PathUtils.getReplacementsPath();
            if (Files.exists(replacementsPath)) {
                try (Stream<Path> paths = Files.walk(replacementsPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                            .forEach(jsonFiles::add);
                }
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to scan replacement files", e);
        }
        return jsonFiles;
    }
}