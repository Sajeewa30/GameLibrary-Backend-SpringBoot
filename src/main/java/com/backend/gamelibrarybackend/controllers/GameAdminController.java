package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    public long fullGameCount = 0;

    @Autowired
    private GameItemRepository gameItemRepository;

    @PostMapping("/addGameItem")
    @Operation(
            summary = "Add a new game",
            description = "Saves a new game entry to the database based on the provided GameItemDTO payload."
    )
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO){

        GameItemEntity newItem = new GameItemEntity(gameItemDTO.getName(),gameItemDTO.getYear(),gameItemDTO.getCompletedYear(),gameItemDTO.isCompleted(),gameItemDTO.isHundredPercent(),gameItemDTO.isFavourite(),gameItemDTO.getSpecialDescription(),gameItemDTO.getImageUrl());

        gameItemRepository.save(newItem);

        return new ResponseEntity<>(gameItemRepository.findAll(),HttpStatus.OK) ;
    }

    @GetMapping("/fullGameCount")
    @Operation(summary = "Get total number of games", description = "Returns the total number of game entries currently stored in the database.")
    public Map<String, Long> getFullGameCount(){
        fullGameCount = gameItemRepository.count();
        return Collections.singletonMap("fullGameCount", fullGameCount );
    }

    @GetMapping("/getGamesByCompletedYear/{year}")
    public ResponseEntity<?> getGamesByCompletedYear(@PathVariable int year) {
        List<GameItemEntity> games = gameItemRepository.findByCompletedYear(year);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/games/byYear/{year}")
    @Operation(summary = "Get games by completed year")
    public List<GameItemEntity> getGamesByYear(@PathVariable int year) {
        return gameItemRepository.findByCompletedYear(year);
    }


    @GetMapping("/getFavouriteGames")
    public ResponseEntity<?> getFavouriteGames() {
        return ResponseEntity.ok(gameItemRepository.findByIsFavouriteTrue());
    }

    @GetMapping("/getHundredPercentCompletedGames")
    public ResponseEntity<?> getHundredPercentGames() {
        return ResponseEntity.ok(gameItemRepository.findByIsHundredPercentTrue());
    }

    @PostMapping("/uploadImage")
    @CrossOrigin
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String uploadDir = "uploads/";
            String fileName = image.getOriginalFilename();
            File uploadPath = new File(uploadDir);

            if (!uploadPath.exists()) uploadPath.mkdirs();

            File destinationFile = new File(uploadDir + fileName);
            image.transferTo(destinationFile);

            String fileUrl = "http://localhost:8080/uploads/" + fileName;
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        }
    }


}


