package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import com.backend.gamelibrarybackend.service.FirebaseStorageService;
import com.backend.gamelibrarybackend.service.S3StorageService;
import org.springframework.dao.DataIntegrityViolationException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping(value = "/admin")
public class GameAdminController {

    @Autowired
    private GameItemRepository gameItemRepository;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @Autowired
    private S3StorageService s3StorageService;

    @PostMapping("/addGameItem")
    @Operation(
            summary = "Add a new game",
            description = "Saves a new game entry to the database based on the provided GameItemDTO payload."
    )
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO, @RequestAttribute("firebaseUid") String userId){

        if (gameItemDTO.getName() == null || gameItemDTO.getName().isBlank() || gameItemDTO.getYear() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Name and year are required."));
        }

        if (gameItemRepository.existsByUserIdAndNameAndYear(userId, gameItemDTO.getName(), gameItemDTO.getYear())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Game already exists for this year."));
        }

        GameItemEntity newItem = new GameItemEntity(
                gameItemDTO.getName(),
                gameItemDTO.getYear(),
                gameItemDTO.getCompletedYear(),
                gameItemDTO.isCompleted(),
                gameItemDTO.isHundredPercent(),
                gameItemDTO.isFavourite(),
                gameItemDTO.getSpecialDescription(),
                gameItemDTO.getImageUrl(),
                userId
        );

        try {
            gameItemRepository.save(newItem);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", "Game already exists for this year."));
        } catch (Exception ex) {
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
        return gameItemRepository.findByUserIdAndCompletedYear(userId, year);
    }


    @GetMapping("/getFavouriteGames")
    public ResponseEntity<?> getFavouriteGames(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemRepository.findByUserIdAndIsFavouriteTrue(userId));
    }

    @GetMapping("/getHundredPercentCompletedGames")
    public ResponseEntity<?> getHundredPercentGames(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemRepository.findByUserIdAndIsHundredPercentTrue(userId));
    }

    @PostMapping("/uploadImage")
    @CrossOrigin
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image, @RequestAttribute("firebaseUid") String userId) {
        try {
            // Prefer S3 if configured; fallback to Firebase Storage.
            if (s3StorageService != null) {
                try {
                    String s3Url = s3StorageService.upload(image, userId);
                    return ResponseEntity.ok(s3Url);
                } catch (RuntimeException ex) {
                    // Fall back to Firebase storage if S3 fails.
                }
            }
            String firebaseUrl = firebaseStorageService.upload(image, userId);
            return ResponseEntity.ok(firebaseUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        }
    }




}


