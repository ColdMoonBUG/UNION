package com.scoder.jusic.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author H
 */
public final class AvMediaStorage {

    private static final Path MEDIA_DIR = Paths.get(System.getProperty("user.dir"), "media");

    private AvMediaStorage() {
    }

    public static Path mediaDir() {
        return MEDIA_DIR;
    }

    public static void ensureDirectory() {
        try {
            Files.createDirectories(MEDIA_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("创建影音文件目录失败", e);
        }
    }

    public static String buildAccessPath(String fileName) {
        return "/av/media/files/" + fileName;
    }
}
