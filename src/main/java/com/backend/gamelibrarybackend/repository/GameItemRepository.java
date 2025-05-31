package com.backend.gamelibrarybackend.repository;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameItemRepository extends JpaRepository<GameItemEntity, Long> {

}
