package com.backend.gamelibrarybackend.repository;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameItemRepository extends JpaRepository<GameItemEntity, Long> {

    List<GameItemEntity> findByUserId(String userId);
    List<GameItemEntity> findByUserIdAndCompletedYear(String userId, int completedYear);
    List<GameItemEntity> findByUserIdAndIsFavouriteTrue(String userId);
    List<GameItemEntity> findByUserIdAndIsHundredPercentTrue(String userId);
    long countByUserId(String userId);
    boolean existsByUserIdAndNameAndYear(String userId, String name, int year);

}
