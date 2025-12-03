package com.backend.gamelibrarybackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameItemUpdateDTO {
    private String name;
    private Integer year;
    private Integer completedYear;
    private Boolean isCompleted;
    private Boolean isHundredPercent;
    private Boolean isFavourite;
    private String specialDescription;
    private String imageUrl;
}
