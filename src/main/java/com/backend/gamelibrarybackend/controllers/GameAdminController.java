package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.GameItemDTO;
import com.backend.gamelibrarybackend.models.GameItemEntity;
import com.backend.gamelibrarybackend.repository.GameItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin
@RequestMapping(value = "/admin")
public class GameAdminController {

    @Autowired
    private GameItemRepository gameItemRepository;

    @PostMapping("/addGameItem")
    public ResponseEntity<?> addGameItem(@RequestBody GameItemDTO gameItemDTO){

        GameItemEntity newItem = new GameItemEntity(gameItemDTO.getName(),gameItemDTO.getYear(),gameItemDTO.getCompletedYear(),gameItemDTO.getSpecialDescription(),gameItemDTO.isCompleted(),gameItemDTO.isHundredPercent(),gameItemDTO.isFavourite());

        gameItemRepository.save(newItem);

        return new ResponseEntity<>(gameItemRepository.findAll(),HttpStatus.OK);
    }
}
