package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.dto.GameItemUpdateDTO;
import com.backend.gamelibrarybackend.dto.MediaDeleteDTO;
import com.backend.gamelibrarybackend.dto.NoteDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import com.backend.gamelibrarybackend.service.LocalStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

@RestController
@RequestMapping(value = "/admin")
public class GameAdminController {

    private static final Logger log = LoggerFactory.getLogger(GameAdminController.class);

    private static final int MIN_YEAR = 1950;

    @Autowired
    private GameItemRepository gameItemRepository;

    @Autowired
    private LocalStorageService localStorageService;

    @PostMapping("/addGameItem")
    @Operation(
            summary = "Add a new game",
            description = "Saves a new game entry to the database based on the provided GameItemDTO payload."
    )
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO, @RequestAttribute("firebaseUid") String userId){

        String name = trimToNull(gameItemDTO.getName());
        String validationError = validate(name, gameItemDTO.getYear(), gameItemDTO.getCompletedYear());
        if (validationError != null) {
            return badRequest(validationError);
        }

        if (gameItemRepository.existsByUserIdAndNameAndYear(userId, name, gameItemDTO.getYear())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Game already exists for this year."));
        }

        GameItemEntity newItem = new GameItemEntity(
                name,
                gameItemDTO.getYear(),
                gameItemDTO.getCompletedYear(),
                gameItemDTO.isCompleted(),
                gameItemDTO.isHundredPercent(),
                gameItemDTO.isFavourite(),
                gameItemDTO.getSpecialDescription(),
                gameItemDTO.getImageUrl(),
                userId
        );

        // If marked completed and no completedYear provided, backfill with the game's year.
        if (newItem.isCompleted() && newItem.getCompletedYear() == 0) {
            newItem.setCompletedYear(newItem.getYear());
        }

        try {
            gameItemRepository.save(newItem);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Game already exists for this year."));
        } catch (Exception ex) {
            log.error("Failed to save game", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Unexpected error saving game."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Game saved successfully.");
        response.put("id", newItem.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/fullGameCount")
    @Operation(summary = "Get total number of games", description = "Returns the total number of game entries currently stored in the database.")
    public Map<String, Long> getFullGameCount(@RequestAttribute("firebaseUid") String userId){
        long fullGameCount = gameItemRepository.countByUserId(userId);
        return Collections.singletonMap("fullGameCount", fullGameCount );
    }



    @GetMapping("/games/byYear/{year}")
    @Operation(summary = "Get games by completed year")
    public List<GameItemEntity> getGamesByYear(@PathVariable int year, @RequestAttribute("firebaseUid") String userId) {
        return gameItemRepository.findCompletedByYearWithFallback(userId, year);
    }

    @GetMapping("/games/toBeCompleted")
    public ResponseEntity<?> getToBeCompleted(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId));
    }


    @GetMapping("/getFavouriteGames")
    public ResponseEntity<?> getFavouriteGames(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemRepository.findByUserIdAndIsFavouriteTrueOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/getHundredPercentCompletedGames")
    public ResponseEntity<?> getHundredPercentGames(@RequestAttribute("firebaseUid") String userId) {
            return ResponseEntity.ok(gameItemRepository.findByUserIdAndIsHundredPercentTrueOrderByCreatedAtDesc(userId));
    }

    // Only cache hits: SQLite can reuse a deleted game's id, and a cached 404
    // would hide the new game for the cache TTL.
    @GetMapping("/games/{id}")
    @Cacheable(value = "gameDetail", key = "#userId + ':' + #id", unless = "#result.statusCode.value() != 200")
    public ResponseEntity<?> getGameById(@PathVariable Long id, @RequestAttribute("firebaseUid") String userId) {
        return gameItemRepository.findByIdAndUserId(id, userId)
                .<ResponseEntity<?>>map(game -> ResponseEntity.ok(game))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    @PostMapping("/uploadImage")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image, @RequestAttribute("firebaseUid") String userId) {
        try {
            return ResponseEntity.ok(localStorageService.upload(image, userId));
        } catch (IOException | RuntimeException e) {
            log.error("Image upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Image upload failed."));
        }
    }

    // Discards a cover uploaded via /uploadImage whose game was then never saved
    // (e.g. a duplicate name), so it doesn't stay orphaned on disk.
    @DeleteMapping("/uploadImage")
    public ResponseEntity<?> discardUploadedImage(@RequestBody MediaDeleteDTO payload, @RequestAttribute("firebaseUid") String userId) {
        if (payload.getUrl() == null || payload.getUrl().isBlank()) {
            return badRequest("Image URL is required.");
        }
        if (gameItemRepository.existsByUserIdAndImageUrl(userId, payload.getUrl())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Image is still used by a game."));
        }
        localStorageService.delete(payload.getUrl(), userId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Image discarded"));
    }

    @DeleteMapping("/games/{id}")
    @CacheEvict(value = "gameDetail", key = "#userId + ':' + #id")
    public ResponseEntity<?> deleteGame(@PathVariable Long id, @RequestAttribute("firebaseUid") String userId) {
        return gameItemRepository.findByIdAndUserId(id, userId)
                .map(entity -> {
                    List<String> files = new ArrayList<>();
                    files.add(entity.getImageUrl());
                    files.addAll(entity.getGallery());
                    files.addAll(entity.getVideos());
                    gameItemRepository.delete(entity);
                    files.forEach(url -> localStorageService.delete(url, userId));
                    return ResponseEntity.ok(Collections.singletonMap("message", "Deleted"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    @PutMapping("/games/{id}")
    @CacheEvict(value = "gameDetail", key = "#userId + ':' + #id")
    public ResponseEntity<?> updateGame(@PathVariable Long id,
                                        @RequestBody GameItemUpdateDTO payload,
                                        @RequestAttribute("firebaseUid") String userId) {

        return gameItemRepository.findByIdAndUserId(id, userId)
                .map(entity -> {
                    String name = trimToNull(payload.getName());
                    int year = payload.getYear() != null ? payload.getYear() : 0;
                    int completedYear = payload.getCompletedYear() != null ? payload.getCompletedYear() : 0;
                    String validationError = validate(name, year, completedYear);
                    if (validationError != null) {
                        return badRequest(validationError);
                    }

                    if (gameItemRepository.existsByUserIdAndNameAndYearAndIdNot(userId, name, year, id)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Collections.singletonMap("message", "Game already exists for this year."));
                    }

                    entity.setName(name);
                    entity.setYear(year);
                    if (payload.getIsCompleted() != null) {
                        entity.setCompleted(payload.getIsCompleted());
                    }
                    if (payload.getCompletedYear() != null) {
                        entity.setCompletedYear(payload.getCompletedYear());
                    }
                    // Ensure completedYear is set when marking as completed
                    if (entity.isCompleted() && entity.getCompletedYear() == 0) {
                        entity.setCompletedYear(entity.getYear());
                    }
                    if (payload.getIsHundredPercent() != null) {
                        entity.setHundredPercent(payload.getIsHundredPercent());
                    }
                    if (payload.getIsFavourite() != null) {
                        entity.setFavourite(payload.getIsFavourite());
                    }
                    if (payload.getSpecialDescription() != null) {
                        entity.setSpecialDescription(payload.getSpecialDescription());
                    }
                    String previousImageUrl = entity.getImageUrl();
                    if (payload.getImageUrl() != null) {
                        entity.setImageUrl(payload.getImageUrl());
                    }

                    try {
                        GameItemEntity saved = gameItemRepository.save(entity);
                        // The old cover is no longer referenced once the new one is saved.
                        if (!Objects.equals(previousImageUrl, saved.getImageUrl())) {
                            localStorageService.delete(previousImageUrl, userId);
                        }
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "Game updated successfully.");
                        response.put("item", saved);
                        return ResponseEntity.ok(response);
                    } catch (DataIntegrityViolationException ex) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Collections.singletonMap("message", "Game already exists for this year."));
                    } catch (Exception ex) {
                        log.error("Failed to update game {}", id, ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Collections.singletonMap("message", "Unexpected error updating game."));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    @PutMapping("/games/{id}/note")
    @CacheEvict(value = "gameDetail", key = "#userId + ':' + #id")
    public ResponseEntity<?> updateNote(@PathVariable Long id,
                                        @RequestBody NoteDTO payload,
                                        @RequestAttribute("firebaseUid") String userId) {
        return gameItemRepository.findByIdAndUserId(id, userId)
                .map(entity -> {
                    entity.setNote(payload.getNote());
                    gameItemRepository.save(entity);
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Note updated");
                    response.put("note", entity.getNote());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    @PostMapping("/games/{id}/media")
    @CacheEvict(value = "gameDetail", key = "#userId + ':' + #id")
    public ResponseEntity<?> uploadMedia(@PathVariable Long id,
                                         @RequestParam("files") MultipartFile[] files,
                                         @RequestParam("type") String type,
                                         @RequestAttribute("firebaseUid") String userId) {
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "No files uploaded."));
        }
        if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid media type."));
        }

        return gameItemRepository.findByIdAndUserId(id, userId)
                .map(entity -> {
                    List<String> urls = new ArrayList<>();
                    for (MultipartFile file : files) {
                        try {
                            urls.add(localStorageService.upload(file, userId));
                        } catch (Exception ex) {
                            log.error("Media upload failed for game {}", id, ex);
                            // Don't leave the files that did upload orphaned on disk.
                            urls.forEach(url -> localStorageService.delete(url, userId));
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Collections.singletonMap("message", "Failed to upload media."));
                        }
                    }

                    if ("image".equalsIgnoreCase(type)) {
                        entity.getGallery().addAll(urls);
                    } else {
                        entity.getVideos().addAll(urls);
                    }
                    GameItemEntity saved = gameItemRepository.save(entity);
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Media uploaded");
                    response.put("gallery", saved.getGallery());
                    response.put("videos", saved.getVideos());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    @DeleteMapping("/games/{id}/media")
    @CacheEvict(value = "gameDetail", key = "#userId + ':' + #id")
    public ResponseEntity<?> deleteMedia(@PathVariable Long id,
                                         @RequestBody MediaDeleteDTO payload,
                                         @RequestAttribute("firebaseUid") String userId) {
        if (payload.getUrl() == null || payload.getUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Media URL is required."));
        }
        if (!"image".equalsIgnoreCase(payload.getType()) && !"video".equalsIgnoreCase(payload.getType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid media type."));
        }

        return gameItemRepository.findByIdAndUserId(id, userId)
                .map(entity -> {
                    boolean removed;
                    if ("image".equalsIgnoreCase(payload.getType())) {
                        removed = entity.getGallery().remove(payload.getUrl());
                    } else {
                        removed = entity.getVideos().remove(payload.getUrl());
                    }
                    if (!removed) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Collections.singletonMap("message", "Media not found on this game."));
                    }
                    GameItemEntity saved = gameItemRepository.save(entity);
                    localStorageService.delete(payload.getUrl(), userId);
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Media deleted");
                    response.put("gallery", saved.getGallery());
                    response.put("videos", saved.getVideos());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Game not found")));
    }

    /** Returns a user-facing error message, or null when the fields are valid. */
    private static String validate(String name, int year, int completedYear) {
        int maxYear = Year.now().getValue() + 5;
        if (name == null || year <= 0) {
            return "Name and year are required.";
        }
        if (year < MIN_YEAR || year > maxYear) {
            return "Year must be between " + MIN_YEAR + " and " + maxYear + ".";
        }
        // 0 means "not completed yet".
        if (completedYear != 0 && (completedYear < MIN_YEAR || completedYear > maxYear)) {
            return "Completed year must be between " + MIN_YEAR + " and " + maxYear + ".";
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("message", message));
    }

}
