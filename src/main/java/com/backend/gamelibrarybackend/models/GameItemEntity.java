package com.backend.gamelibrarybackend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
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

    public GameItemEntity() {

    }

    public GameItemEntity(String name, int year, int completedYear, boolean isCompleted, boolean isHundredPercent, boolean isFavourite , String specialDescription , String imageUrl) {
        this.name = name;
        this.year = year;
        this.completedYear = completedYear;
        this.isCompleted = isCompleted;
        this.isHundredPercent = isHundredPercent;
        this.isFavourite = isFavourite;
        this.specialDescription = specialDescription;
        this.imageUrl = imageUrl;
    }
}
