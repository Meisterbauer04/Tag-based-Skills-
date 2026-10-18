package com.meisterbauer.tagbasedskills.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// Resolves this mod's configuration locations and creates missing files from defaults.
public final class ConfigPaths {
    private static final String CONFIG_DIRECTORY = "TagBasedSkills";

    // Resolves a file in the current {@value #CONFIG_DIRECTORY} configuration directory.
    public static Path resolve(String fileName) {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIRECTORY).resolve(fileName);
    }

    // Creates {@code path} from {@code defaultContent} only when it does not already exist.
    public static void createIfAbsent(Path path, String defaultContent) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(path, defaultContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
}
