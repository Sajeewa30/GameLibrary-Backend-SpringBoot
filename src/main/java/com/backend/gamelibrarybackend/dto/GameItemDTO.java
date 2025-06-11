package com.backend.gamelibrarybackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameItemDTO {

    private String name;
    private int year;
    private int completedYear;

    @JsonProperty("isCompleted")
    private boolean isCompleted;

    @JsonProperty("isHundredPercent")
    private boolean isHundredPercent;

    @JsonProperty("isFavourite")
    private boolean isFavourite;

    private String specialDescription;

}
