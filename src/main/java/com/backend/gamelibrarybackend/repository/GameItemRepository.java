package com.backend.gamelibrarybackend.repository;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameItemRepository extends JpaRepository<GameItemEntity, Long> {

    List<GameItemEntity> findByCompletedYear(int completedYear);
    List<GameItemEntity> findByIsFavouriteTrue();
    List<GameItemEntity> findByIsHundredPercentTrue();

}
