package com.backend.gamelibrarybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiPublicResponse {
    private int year;
    private List<OpenAiGameItem> items;
}
