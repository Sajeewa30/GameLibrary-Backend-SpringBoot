package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.service.FirebaseStorageService;
import com.backend.gamelibrarybackend.service.GameItemService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping(value = "/admin")
public class GameAdminController {

    @Autowired
    private GameItemService gameItemService;

    @Autowired
    private FirebaseStorageService firebaseStorageService;

    @PostMapping("/addGameItem")
    @Operation(
            summary = "Add a new game",
            description = "Saves a new game entry to the database based on the provided GameItemDTO payload."
    )
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO, @RequestAttribute("firebaseUid") String userId){

        List<GameItemEntity> items = gameItemService.addGame(userId, gameItemDTO);
        return new ResponseEntity<>(items,HttpStatus.OK) ;
    }

    @GetMapping("/fullGameCount")
    @Operation(summary = "Get total number of games", description = "Returns the total number of game entries currently stored in the database.")
    public Map<String, Long> getFullGameCount(@RequestAttribute("firebaseUid") String userId){
        long fullGameCount = gameItemService.count(userId);
        return Collections.singletonMap("fullGameCount", fullGameCount );
    }



    @GetMapping("/games/byYear/{year}")
    @Operation(summary = "Get games by completed year")
    public List<GameItemEntity> getGamesByYear(@PathVariable int year, @RequestAttribute("firebaseUid") String userId) {
        return gameItemService.findByYear(userId, year);
    }


    @GetMapping("/getFavouriteGames")
    public ResponseEntity<?> getFavouriteGames(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemService.findFavourites(userId));
    }

    @GetMapping("/getHundredPercentCompletedGames")
    public ResponseEntity<?> getHundredPercentGames(@RequestAttribute("firebaseUid") String userId) {
        return ResponseEntity.ok(gameItemService.findHundredPercent(userId));
    }

    @PostMapping("/uploadImage")
    @CrossOrigin
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile image, @RequestAttribute("firebaseUid") String userId) {
        try {
            String fileUrl = firebaseStorageService.upload(image, userId);
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed: " + e.getMessage());
        }
    }




}


