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
    private String specialDescription;
    @Getter
    @Setter
    private boolean isCompleted;
    @Getter
    @Setter
    private boolean isHundredPercent;

    public GameItemEntity(String name, int year, int completedYear, String specialDescription, boolean isCompleted, boolean isHundredPercent) {
        this.name = name;
        this.year = year;
        this.completedYear = completedYear;
        this.specialDescription = specialDescription;
        this.isCompleted = isCompleted;
        this.isHundredPercent = isHundredPercent;

    }
}
