package com.backend.gamelibrarybackend;

import com.backend.gamelibrarybackend.service.LocalStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageServiceTest {

    private static final String BASE_URL = "http://localhost:8080/uploads";

    @TempDir
    Path dir;

    @Test
    void uploadIgnoresUnsafeCharactersInTheOriginalName() throws Exception {
        LocalStorageService storage = new LocalStorageService(dir.toString(), BASE_URL);
        String url = storage.upload(new MockMultipartFile("image", "my #1 game?:100%.PNG", "image/png", new byte[]{1}), "user1");

        assertTrue(url.startsWith(BASE_URL + "/user1/"));
        assertTrue(url.endsWith(".png"));
        assertFalse(url.substring(BASE_URL.length()).matches(".*[#?:% ].*"));
    }

    @Test
    void deleteRemovesOnlyTheOwnersFile() throws Exception {
        LocalStorageService storage = new LocalStorageService(dir.toString(), BASE_URL);
        String url = storage.upload(new MockMultipartFile("image", "cover.jpg", "image/jpeg", new byte[]{1}), "user1");
        Path file = dir.resolve("user1").resolve(url.substring(url.lastIndexOf('/') + 1));
        Files.writeString(dir.resolve("secret.txt"), "keep");

        storage.delete(url, "user2");                                  // another user's file
        storage.delete(BASE_URL + "/user1/../secret.txt", "user1");    // escapes the folder
        storage.delete("https://example.com/cover.jpg", "user1");      // external URL
        assertTrue(Files.exists(file));
        assertTrue(Files.exists(dir.resolve("secret.txt")));

        storage.delete(url, "user1");
        assertFalse(Files.exists(file));
    }
}
