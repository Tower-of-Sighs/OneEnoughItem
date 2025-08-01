package com.mafuyu404.oneenoughitem.client.gui.util;

import com.mafuyu404.oneenoughitem.Oneenoughitem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PathUtils {

    /**
     * 获取数据包路径
     * @param datapackName 数据包名称，如果为空则使用默认的"OEI"
     * @return 数据包路径
     */
    public static Path getDatapackPath(String datapackName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            Path datapacksPath = worldPath.resolve("datapacks");

            if (datapackName == null || datapackName.trim().isEmpty()) {
                return datapacksPath.resolve("OEI");
            } else {
                return datapacksPath.resolve(datapackName.trim());
            }
        } else if (minecraft.level != null) {
            try {
                Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
                Path worldPath = findCurrentWorldPath(savesPath);
                if (worldPath != null) {
                    Path datapacksPath = worldPath.resolve("datapacks");

                    if (datapackName == null || datapackName.trim().isEmpty()) {
                        return datapacksPath.resolve("OEI");
                    } else {
                        return datapacksPath.resolve(datapackName.trim());
                    }
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.warn("Failed to determine world path", e);
            }
        }

        Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
        return savesPath.resolve("datapacks/OEI");
    }

    /**
     * 获取替换文件目录路径
     * @return 替换文件目录路径
     */
    public static Path getReplacementsPath() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() != null) {
            Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            return worldPath.resolve("datapacks").resolve("OEI").resolve("data/oneenoughitem/oneenoughitem/replacements");
        } else if (minecraft.level != null) {
            try {
                Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
                Path worldPath = findCurrentWorldPath(savesPath);
                if (worldPath != null) {
                    return worldPath.resolve("datapacks").resolve("OEI").resolve("data/oneenoughitem/oneenoughitem/replacements");
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.warn("Failed to determine world path", e);
            }
        }

        Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
        return savesPath.resolve("datapacks/OEI/data/oneenoughitem/oneenoughitem/replacements");
    }

    /**
     * 获取datapacks根目录路径
     * @return datapacks目录路径
     */
    public static Path getDatapacksPath() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() != null) {
            Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            return worldPath.resolve("datapacks");
        } else if (minecraft.level != null) {
            try {
                Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
                Path worldPath = findCurrentWorldPath(savesPath);
                if (worldPath != null) {
                    return worldPath.resolve("datapacks");
                }
            } catch (Exception e) {
                Oneenoughitem.LOGGER.warn("Failed to determine world path", e);
            }
        }

        Path savesPath = minecraft.gameDirectory.toPath().resolve("saves");
        return savesPath.resolve("datapacks");
    }

    /**
     * 扫描所有datapacks子文件夹中的JSON文件
     * @return 包含文件信息的列表
     */
    public static List<FileInfo> scanAllReplacementFiles() {
        List<FileInfo> jsonFiles = new ArrayList<>();
        try {
            Path datapacksPath = getDatapacksPath();
            if (!Files.exists(datapacksPath)) {
                Oneenoughitem.LOGGER.warn("Datapacks directory does not exist: {}", datapacksPath);
                return jsonFiles;
            }

            // 遍历datapacks目录下的所有子文件夹
            try (Stream<Path> datapackDirs = Files.list(datapacksPath)) {
                datapackDirs.filter(Files::isDirectory)
                        .forEach(datapackDir -> {
                            Path replacementsPath = datapackDir.resolve("data/oneenoughitem/oneenoughitem/replacements");
                            if (Files.exists(replacementsPath)) {
                                scanJsonFilesInDirectory(replacementsPath, datapackDir, jsonFiles);
                            }
                        });
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to scan replacement files", e);
        }
        return jsonFiles;
    }

    private static void scanJsonFilesInDirectory(Path replacementsPath, Path datapackRoot, List<FileInfo> jsonFiles) {
        try (Stream<Path> paths = Files.walk(replacementsPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String displayName = fileName.endsWith(".json") ?
                                fileName.substring(0, fileName.length() - 5) : fileName;

                        String datapackName = datapackRoot.getFileName().toString();
                        Path relativePath = replacementsPath.relativize(path.getParent());
                        String relativePathStr = relativePath.toString().equals(".") ? "" : relativePath.toString();

                        String fullPath = "datapacks/" + datapackName + "/data/oneenoughitem/oneenoughitem/replacements" +
                                (relativePathStr.isEmpty() ? "" : "/" + relativePathStr);

                        jsonFiles.add(new FileInfo(displayName, path, fullPath, datapackName));
                    });
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Failed to scan files in directory: {}", replacementsPath, e);
        }
    }

    public static Path findCurrentWorldPath(Path savesPath) {
        try {
            if (!Files.exists(savesPath)) {
                return null;
            }

            try (Stream<Path> stream = Files.list(savesPath)) {
                return stream
                        .filter(Files::isDirectory)
                        .filter(path -> Files.exists(path.resolve("level.dat")))
                        .max((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .orElse(null);
            }
        } catch (IOException e) {
            Oneenoughitem.LOGGER.error("Error finding current world path", e);
            return null;
        }
    }

        public record FileInfo(String displayName, Path filePath, String fullPath, String datapackName) {
    }
}