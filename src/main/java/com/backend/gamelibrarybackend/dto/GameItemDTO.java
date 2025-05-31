package com.backend.gamelibrarybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GameItemDTO {

    private String name;

    private int year;

    private int completedYear;

    private String specialDescription;

    private boolean isCompleted;

    private boolean isHundredPercent;

    private boolean isFavourite;

}
