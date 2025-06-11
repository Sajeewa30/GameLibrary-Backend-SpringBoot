package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping(value = "/admin")
public class GameAdminController {

    public long fullGameCount = 0;

    @Autowired
    private GameItemRepository gameItemRepository;

    @PostMapping("/addGameItem")
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO){

        GameItemEntity newItem = new GameItemEntity(gameItemDTO.getName(),gameItemDTO.getYear(),gameItemDTO.getCompletedYear(),gameItemDTO.isCompleted(),gameItemDTO.isHundredPercent(),gameItemDTO.isFavourite(),gameItemDTO.getSpecialDescription());

        gameItemRepository.save(newItem);

        return new ResponseEntity<>(gameItemRepository.findAll(),HttpStatus.OK) ;
    }

    @GetMapping("/fullGameCount")
    public Map<String, Long> getFullGameCount(){
        fullGameCount = gameItemRepository.count();
        return Collections.singletonMap("fullGameCount", fullGameCount );
    }
}


