package com.backend.gamelibrarybackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded images/videos on the local filesystem instead of the cloud.
 * Files are written under {storage.local.dir}/{userId}/ and served back by
 * {@code WebConfig} at /uploads/**. Active only under the 'local' profile, so it
 * transparently replaces S3/Firebase storage when running the app locally.
 */
@Service
@Profile("local")
public class LocalStorageService {

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
        String originalName = file.getOriginalFilename();
        String safeName = (originalName != null) ? Paths.get(originalName).getFileName().toString() : "upload";
        String fileName = System.currentTimeMillis() + "_" + safeName;

        Path userDir = baseDir.resolve(userId);
        Files.createDirectories(userDir);
        Path target = userDir.resolve(fileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return publicBaseUrl + "/" + userId + "/" + fileName;
    }
}
