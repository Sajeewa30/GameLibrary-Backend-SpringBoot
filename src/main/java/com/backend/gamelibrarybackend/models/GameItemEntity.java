package com.backend.gamelibrarybackend.models;

import lombok.Getter;
import lombok.Setter;

public class GameItemEntity {

    @Getter
    @Setter
    private String id;
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
    private String userId;

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
