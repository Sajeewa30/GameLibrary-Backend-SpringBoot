package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping(value = "/admin")
public class GameAdminController {

    @Autowired
    private GameItemRepository gameItemRepository;

    @PostMapping("/addGameItem")
    @Operation(
            summary = "Add a new game",
            description = "Saves a new game entry to the database based on the provided GameItemDTO payload."
    )
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO, @RequestAttribute("firebaseUid") String userId){

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

        gameItemRepository.save(newItem);

        return new ResponseEntity<>(gameItemRepository.findByUserId(userId),HttpStatus.OK) ;
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
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + File.separator + "uploads" + File.separator + userId;
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();

            File uploadPath = new File(uploadDir);

            if (!uploadPath.exists()) {
                boolean created = uploadPath.mkdirs();
                if (!created) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create upload directory.");
                }
            }

            File destinationFile = new File(uploadPath, fileName);
            image.transferTo(destinationFile);

            String fileUrl = "http://localhost:8080/uploads/" + userId + "/" + fileName;
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        }
    }




}


