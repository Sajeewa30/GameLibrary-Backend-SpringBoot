package com.backend.gamelibrarybackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores uploaded images/videos on the local filesystem. Files are written under
 * {storage.local.dir}/{userId}/ and served back by {@code WebConfig} at /uploads/**.
 */
@Service
public class LocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalStorageService(@Value("${storage.local.dir:uploads}") String dir,
                               @Value("${storage.local.public-base-url:http://localhost:8080/uploads}") String publicBaseUrl) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        // Trim a trailing slash so we can join path segments with '/' predictably.
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    public String upload(MultipartFile file, String userId) throws IOException {
        // Never trust the client's file name: characters like '#', '?', '%' or ':'
        // break the public URL (or the Windows path). Keep only a safe extension.
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + safeExtension(file.getOriginalFilename());

        Path userDir = baseDir.resolve(userId);
        Files.createDirectories(userDir);
        Path target = userDir.resolve(fileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return publicBaseUrl + "/" + userId + "/" + fileName;
    }

    /**
     * Deletes a file previously returned by {@link #upload}. Only URLs inside this
     * user's own upload folder are touched; anything else (an external URL, another
     * user's file, a path that escapes the folder) is ignored. Never throws, so a
     * failed cleanup can't fail the request that triggered it.
     */
    public void delete(String url, String userId) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        String userPrefix = publicBaseUrl + "/" + userId + "/";
        if (!url.startsWith(userPrefix)) {
            return;
        }
        Path userDir = baseDir.resolve(userId).normalize();
        Path target = userDir.resolve(url.substring(userPrefix.length())).normalize();
        if (!target.startsWith(userDir) || target.equals(userDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not delete uploaded file {}: {}", target, ex.getMessage());
        }
    }

    private static String safeExtension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        String ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
    }
}
