package com.backend.gamelibrarybackend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MediaDeleteDTO {
    private String url;
    private String type; // "image" or "video"
}
