package com.backend.gamelibrarybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiGameItem {
    private String name;
    private int releaseYear;
    private String summary;
    private List<String> platforms;
    private List<String> genres;
    private String coverUrl;
}
