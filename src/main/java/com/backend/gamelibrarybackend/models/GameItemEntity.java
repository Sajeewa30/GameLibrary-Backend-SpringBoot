package com.backend.gamelibrarybackend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "game_item_entity",
        indexes = {
                @Index(name = "idx_game_item_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(name = "uc_user_name_year", columnNames = {"user_id", "name", "year"})
        }
)
public class GameItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private int year;
    @Getter
    @Setter
    private int completedYear;
    @Getter
    @Setter
    private boolean isCompleted;
    @Getter
    @Setter
    private boolean isHundredPercent;
    @Getter
    @Setter
    private boolean isFavourite;
    @Getter
    @Setter
    private String specialDescription;
    @Getter
    @Setter
    private String imageUrl;
    @Getter
    @Setter
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;
    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public GameItemEntity() {

    }

    public GameItemEntity(String name, int year, int completedYear, boolean isCompleted, boolean isHundredPercent, boolean isFavourite , String specialDescription , String imageUrl, String userId) {
        this.name = name;
        this.year = year;
        this.completedYear = completedYear;
        this.isCompleted = isCompleted;
        this.isHundredPercent = isHundredPercent;
        this.isFavourite = isFavourite;
        this.specialDescription = specialDescription;
        this.imageUrl = imageUrl;
        this.userId = userId;
    }
}
