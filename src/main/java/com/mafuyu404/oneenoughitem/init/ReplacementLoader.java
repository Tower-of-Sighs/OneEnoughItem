package com.mafuyu404.oneenoughitem.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReplacementLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("oei");
    private static final Path KJSDATA_DIR = FMLPaths.GAMEDIR.get()
            .resolve("kubejs")
            .resolve("data")
            .resolve("oei")
            .resolve("replacements");

    public static List<Replacement> loadAll() {
        List<Replacement> allReplacement = new ArrayList<>();

        allReplacement.addAll(loadFromDir(CONFIG_DIR));
        if (ModList.get().isLoaded("kubejs")) {
            if (Config.READ_KUBEJS_DATA.get()) allReplacement.addAll(loadFromDir(KJSDATA_DIR));
        }

        return allReplacement;
    }

    private static List<Replacement> loadFromDir(Path path) {
        List<Replacement> allReplacement = new ArrayList<>();

        // 确保目录存在
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return allReplacement;
        }

        // 遍历所有JSON文件
        try (var stream = Files.newDirectoryStream(path, "*.json")) {
            for (Path file : stream) {
                allReplacement.addAll(loadRecipesFromFile(file));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allReplacement;
    }

    private static Collection<? extends Replacement> loadRecipesFromFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return GSON.fromJson(reader, new TypeToken<List<Replacement>>(){}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
