package com.backend.gamelibrarybackend.controllers;

import com.backend.gamelibrarybackend.dto.OpenAiGameItem;
import com.backend.gamelibrarybackend.dto.OpenAiPublicResponse;
import com.backend.gamelibrarybackend.service.OpenAiClientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/public/ai")
public class PublicAiController {

    private final OpenAiClientService openAiClientService;

    public PublicAiController(OpenAiClientService openAiClientService) {
        this.openAiClientService = openAiClientService;
    }

    @GetMapping("/games")
    public OpenAiPublicResponse getGames(@RequestParam int year,
                                         @RequestParam(defaultValue = "100") int count,
                                         @RequestParam(required = false) String search) {
        int currentYear = Year.now().getValue();
        if (year < 1975 || year > currentYear) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "year must be between 1975 and " + currentYear);
        }
        if (count < 1 || count > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count must be between 1 and 100");
        }

        List<OpenAiGameItem> items;
        try {
            items = openAiClientService.fetchGames(year, count);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI request failed");
        }

        if (search != null && !search.isBlank()) {
            String needle = search.toLowerCase(Locale.ROOT);
            items = items.stream()
                    .filter(item -> matchesSearch(item, needle))
                    .toList();
        }

        if (items.size() > count) {
            items = items.subList(0, count);
        }

        return new OpenAiPublicResponse(year, items);
    }

    private boolean matchesSearch(OpenAiGameItem item, String needle) {
        if (item == null) {
            return false;
        }
        if (containsIgnoreCase(item.getName(), needle) || containsIgnoreCase(item.getSummary(), needle)) {
            return true;
        }
        if (item.getPlatforms() != null) {
            for (String platform : item.getPlatforms()) {
                if (containsIgnoreCase(platform, needle)) {
                    return true;
                }
            }
        }
        if (item.getGenres() != null) {
            for (String genre : item.getGenres()) {
                if (containsIgnoreCase(genre, needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String value, String needle) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
