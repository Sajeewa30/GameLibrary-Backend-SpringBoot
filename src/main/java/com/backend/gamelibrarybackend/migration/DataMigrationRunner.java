package com.backend.gamelibrarybackend.migration;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * One-shot data mover for migrating the game library between databases — used to
 * lift every game out of the old Railway MySQL and into the new local SQLite file.
 *
 * It does nothing during normal operation. It acts only when one of these program
 * arguments is supplied, then shuts the JVM down:
 *
 *   --app.migrate.export=games-export.json   read every game from the CURRENT
 *                                            datasource and write it to a JSON file
 *   --app.migrate.import=games-export.json   read that JSON file and save every
 *                                            game into the CURRENT datasource
 *
 * Both directions go through JPA, so types (timestamps, booleans, and the gallery
 * and video collections) are converted correctly regardless of the underlying DB.
 * Driven by scripts/migrate-from-railway.ps1 — see LOCAL-SETUP.md.
 */
@Component
public class DataMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);

    private final GameItemRepository repository;
    private final ObjectMapper objectMapper;
    private final String exportFile;
    private final String importFile;

    public DataMigrationRunner(GameItemRepository repository,
                               ObjectMapper objectMapper,
                               @Value("${app.migrate.export:}") String exportFile,
                               @Value("${app.migrate.import:}") String importFile) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.exportFile = exportFile;
        this.importFile = importFile;
    }

    @Override
    public void run(String... args) throws Exception {
        if (StringUtils.hasText(exportFile)) {
            export(exportFile);
            stop();
        } else if (StringUtils.hasText(importFile)) {
            doImport(importFile);
            stop();
        }
    }

    private void export(String path) throws Exception {
        List<GameDto> dtos = repository.findAll().stream().map(DataMigrationRunner::toDto).toList();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(path), dtos);
        log.info("MIGRATION EXPORT: wrote {} games to {}", dtos.size(), path);
    }

    private void doImport(String path) throws Exception {
        List<GameDto> dtos = objectMapper.readValue(
                new File(path),
                objectMapper.getTypeFactory().constructCollectionType(List.class, GameDto.class));
        int saved = 0;
        int skipped = 0;
        for (GameDto dto : dtos) {
            // Idempotent: the unique (user, name, year) constraint also guards this.
            if (repository.existsByUserIdAndNameAndYear(dto.userId(), dto.name(), dto.year())) {
                skipped++;
                continue;
            }
            repository.save(toEntity(dto));
            saved++;
        }
        log.info("MIGRATION IMPORT: saved {} games, skipped {} already present, from {}", saved, skipped, path);
    }

    private void stop() {
        log.info("MIGRATION complete. Shutting down.");
        System.exit(0);
    }

    private static GameDto toDto(GameItemEntity e) {
        return new GameDto(
                e.getId(), e.getName(), e.getYear(), e.getCompletedYear(),
                e.isCompleted(), e.isHundredPercent(), e.isFavourite(),
                e.getSpecialDescription(), e.getImageUrl(), e.getUserId(), e.getNote(),
                e.getGallery(), e.getVideos(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static GameItemEntity toEntity(GameDto d) {
        GameItemEntity e = new GameItemEntity(
                d.name(), d.year(), d.completedYear(),
                d.completed(), d.hundredPercent(), d.favourite(),
                d.specialDescription(), d.imageUrl(), d.userId());
        e.setNote(d.note());
        if (d.gallery() != null) {
            e.getGallery().addAll(d.gallery());
        }
        if (d.videos() != null) {
            e.getVideos().addAll(d.videos());
        }
        // Preserve original creation time so "recently added" ordering survives.
        if (d.createdAt() != null) {
            e.setCreatedAt(d.createdAt());
        }
        return e;
    }

    public record GameDto(
            Long id, String name, int year, int completedYear,
            boolean completed, boolean hundredPercent, boolean favourite,
            String specialDescription, String imageUrl, String userId, String note,
            List<String> gallery, List<String> videos,
            Instant createdAt, Instant updatedAt) {
    }
}
