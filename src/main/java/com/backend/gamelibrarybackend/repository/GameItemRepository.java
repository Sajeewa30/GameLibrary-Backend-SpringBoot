package com.backend.gamelibrarybackend.repository;

import com.backend.gamelibrarybackend.models.GameItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameItemRepository extends JpaRepository<GameItemEntity, Long> {

    List<GameItemEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    List<GameItemEntity> findByUserIdAndCompletedYearOrderByCreatedAtDesc(String userId, int completedYear);
    List<GameItemEntity> findByUserIdAndIsFavouriteTrueOrderByCreatedAtDesc(String userId);
    List<GameItemEntity> findByUserIdAndIsHundredPercentTrueOrderByCreatedAtDesc(String userId);
    List<GameItemEntity> findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(String userId);
    @Query(value = """
            SELECT g FROM GameItemEntity g
            WHERE g.userId = :userId
              AND g.isCompleted = true
              AND (g.completedYear = :year OR (g.completedYear = 0 AND g.year = :year))
            ORDER BY g.createdAt DESC
            """)
    List<GameItemEntity> findCompletedByYearWithFallback(@Param("userId") String userId, @Param("year") int year);
    long countByUserId(String userId);
    boolean existsByUserIdAndNameAndYear(String userId, String name, int year);
    boolean existsByUserIdAndNameAndYearAndIdNot(String userId, String name, int year, Long id);
    java.util.Optional<GameItemEntity> findByIdAndUserId(Long id, String userId);

}
