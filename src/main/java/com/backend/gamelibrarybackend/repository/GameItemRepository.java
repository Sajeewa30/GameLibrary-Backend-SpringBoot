package com.backend.gamelibrarybackend.repository;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameItemRepository extends JpaRepository<GameItemEntity, Long> {

    List<GameItemEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    List<GameItemEntity> findByUserIdAndCompletedYearOrderByCreatedAtDesc(String userId, int completedYear);
    List<GameItemEntity> findByUserIdAndIsFavouriteTrueOrderByCreatedAtDesc(String userId);
    List<GameItemEntity> findByUserIdAndIsHundredPercentTrueOrderByCreatedAtDesc(String userId);
    long countByUserId(String userId);
    boolean existsByUserIdAndNameAndYear(String userId, String name, int year);
    java.util.Optional<GameItemEntity> findByIdAndUserId(Long id, String userId);

}
