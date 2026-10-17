package com.backend.gamelibrarybackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mirrors the library to Google Drive with rclone, on demand (the "Back up to
 * Google Drive" button). Only new or changed files are uploaded; the Drive copy
 * is an exact mirror, so files deleted locally are removed from it too (rclone
 * moves them to Drive's trash, which keeps them for 30 days).
 *
 * Drive layout under {backup.drive.remote}:
 *   gamelibrary.db   consistent snapshot of the SQLite database
 *   uploads/         mirror of the local uploads folder
 *
 * The site itself never reads from Drive; this is a backup only.
 */
@Service
public class DriveBackupService {

    private static final Logger log = LoggerFactory.getLogger(DriveBackupService.class);

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final String rclonePathSetting;
    private final String remote;
    private final Path uploadsDir;
    private final Path workDir;
    private final Path statusFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "drive-backup");
        t.setDaemon(true);
        return t;
    });

    private Map<String, Object> status;

    public DriveBackupService(DataSource dataSource,
                              ObjectMapper objectMapper,
                              @Value("${backup.drive.rclone-path:}") String rclonePath,
                              @Value("${backup.drive.remote:gdrive:GameLibrary}") String remote,
                              @Value("${storage.local.dir:uploads}") String uploadsDir,
                              @Value("${backup.drive.work-dir:backups}") String workDir) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.rclonePathSetting = rclonePath;
        this.remote = remote.endsWith("/") ? remote.substring(0, remote.length() - 1) : remote;
        this.uploadsDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        this.workDir = Paths.get(workDir).toAbsolutePath().normalize();
        this.statusFile = this.workDir.resolve("last-drive-backup.json");
        this.status = loadLastStatus();
    }

    /** Current or last backup status (a copy, safe to serialize). */
    public synchronized Map<String, Object> getStatus() {
        return new LinkedHashMap<>(status);
    }

    /** Starts a backup unless one is already running. Returns the status either way. */
    public synchronized Map<String, Object> start() {
        if ("running".equals(status.get("state"))) {
            return getStatus();
        }
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("state", "running");
        s.put("step", "Preparing");
        s.put("startedAt", Instant.now().toString());
        s.put("bytesDone", 0L);
        s.put("bytesTotal", 0L);
        status = s;
        executor.submit(this::run);
        return getStatus();
    }

    private void run() {
        try {
            String rclone = resolveRclone();

            update("step", "Snapshotting database");
            Path snapshot = snapshotDatabase();

            update("step", "Uploading database");
            exec(List.of(rclone, "copyto", snapshot.toString(), remote + "/gamelibrary.db"), false);

            update("step", "Uploading images and videos");
            Files.createDirectories(uploadsDir);
            Map<String, Long> totals = exec(List.of(
                    rclone, "sync", uploadsDir.toString(), remote + "/uploads",
                    "--use-json-log", "--stats", "1s", "--stats-log-level", "NOTICE",
                    "--transfers", "4"), true);

            finish("success", String.format("%d uploaded, %d removed from Drive",
                    totals.getOrDefault("transfers", 0L), totals.getOrDefault("deletes", 0L)), totals);
        } catch (Exception ex) {
            log.error("Google Drive backup failed", ex);
            finish("failed", ex.getMessage() == null ? ex.toString() : ex.getMessage(), Map.of());
        }
    }

    /** SQLite VACUUM INTO writes a consistent copy even while the app is using the database. */
    private Path snapshotDatabase() throws Exception {
        Files.createDirectories(workDir);
        Path snapshot = workDir.resolve("drive-snapshot.db");
        Files.deleteIfExists(snapshot); // VACUUM INTO refuses to overwrite
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("VACUUM INTO '" + snapshot.toString().replace("'", "''") + "'");
        }
        return snapshot;
    }

    /**
     * Runs rclone and waits for it. With {@code trackProgress}, parses rclone's JSON
     * stats lines to report progress; returns the last stats seen.
     */
    private Map<String, Long> exec(List<String> command, boolean trackProgress) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        Map<String, Long> totals = new LinkedHashMap<>();
        Deque<String> tail = new ArrayDeque<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (tail.size() == 5) tail.removeFirst();
                tail.addLast(line);
                if (trackProgress) {
                    readStats(line, totals);
                }
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("rclone exited with code " + exit + ": " + lastErrorLine(tail));
        }
        return totals;
    }

    private void readStats(String line, Map<String, Long> totals) {
        if (!line.startsWith("{")) return;
        try {
            JsonNode stats = objectMapper.readTree(line).get("stats");
            if (stats == null) return;
            for (String key : List.of("bytes", "totalBytes", "transfers", "totalTransfers", "deletes", "errors")) {
                if (stats.has(key)) totals.put(key, stats.get(key).asLong());
            }
            synchronized (this) {
                status.put("bytesDone", totals.getOrDefault("bytes", 0L));
                status.put("bytesTotal", totals.getOrDefault("totalBytes", 0L));
                status.put("filesDone", totals.getOrDefault("transfers", 0L));
                status.put("filesTotal", totals.getOrDefault("totalTransfers", 0L));
            }
        } catch (IOException ignored) {
            // Not a stats line.
        }
    }

    private static String lastErrorLine(Deque<String> tail) {
        List<String> lines = new ArrayList<>(tail);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String l = lines.get(i);
            if (l.contains("ERROR") || l.contains("error") || l.contains("Failed")) {
                return l.length() > 300 ? l.substring(0, 300) : l;
            }
        }
        return lines.isEmpty() ? "no output" : lines.get(lines.size() - 1);
    }

    private synchronized void update(String key, Object value) {
        status.put(key, value);
    }

    private synchronized void finish(String state, String message, Map<String, Long> totals) {
        status.put("state", state);
        status.put("message", message);
        status.put("finishedAt", Instant.now().toString());
        status.remove("step");
        if ("success".equals(state)) {
            status.put("filesUploaded", totals.getOrDefault("transfers", 0L));
            status.put("filesDeleted", totals.getOrDefault("deletes", 0L));
            status.put("bytesUploaded", totals.getOrDefault("bytes", 0L));
        }
        try {
            Files.createDirectories(workDir);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(statusFile.toFile(), status);
        } catch (IOException ex) {
            log.warn("Could not save backup status: {}", ex.getMessage());
        }
    }

    private Map<String, Object> loadLastStatus() {
        try {
            if (Files.exists(statusFile)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> saved = objectMapper.readValue(statusFile.toFile(), LinkedHashMap.class);
                // A backup that was running when the app stopped didn't finish.
                if ("running".equals(saved.get("state"))) {
                    saved.put("state", "failed");
                    saved.put("message", "The app stopped while this backup was running.");
                }
                return saved;
            }
        } catch (IOException ex) {
            log.warn("Could not read last backup status: {}", ex.getMessage());
        }
        Map<String, Object> idle = new LinkedHashMap<>();
        idle.put("state", "never");
        return idle;
    }

    /**
     * The configured path, else "rclone" from PATH, else a winget install under
     * %LOCALAPPDATA% (winget's portable install only updates PATH for new shells).
     */
    private String resolveRclone() throws IOException {
        if (StringUtils.hasText(rclonePathSetting)) {
            return rclonePathSetting;
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                for (String name : List.of("rclone.exe", "rclone")) {
                    Path candidate = Paths.get(dir.trim(), name);
                    if (Files.isRegularFile(candidate)) return candidate.toString();
                }
            }
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            Path packages = Paths.get(localAppData, "Microsoft", "WinGet", "Packages");
            if (Files.isDirectory(packages)) {
                try (DirectoryStream<Path> pkgs = Files.newDirectoryStream(packages, "Rclone.Rclone_*")) {
                    for (Path pkg : pkgs) {
                        try (DirectoryStream<Path> versions = Files.newDirectoryStream(pkg, "rclone-*")) {
                            for (Path v : versions) {
                                Path exe = v.resolve("rclone.exe");
                                if (Files.isRegularFile(exe)) return exe.toString();
                            }
                        }
                    }
                }
            }
        }
        throw new IOException("rclone is not installed or not on PATH. Install it, or set backup.drive.rclone-path.");
    }
}
