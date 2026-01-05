package com.backend.gamelibrarybackend.service;

import com.backend.gamelibrarybackend.config.OpenAiProperties;
import com.backend.gamelibrarybackend.dto.OpenAiGameItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OpenAiClientService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT = "You are a video game historian. Return only valid JSON. No extra text.";
    private static final Logger logger = LoggerFactory.getLogger(OpenAiClientService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;

    public OpenAiClientService(ObjectMapper objectMapper,
                               OpenAiProperties openAiProperties,
                               RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(10))
                .setReadTimeout(java.time.Duration.ofSeconds(20))
                .build();
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
    }

    public List<OpenAiGameItem> fetchGames(int year, int count) {
        String userPrompt = buildUserPrompt(year, count, false);
        try {
            return parseGames(callOpenAi(userPrompt), year, count);
        } catch (IllegalStateException ex) {
            String strictPrompt = buildUserPrompt(year, count, true);
            return parseGames(callOpenAi(strictPrompt), year, count);
        }
    }

    private String buildUserPrompt(int year, int count, boolean strict) {
        String prompt = "List the top " + count + " most notable video games released in " + year + ".\n"
                + "Return JSON array items with fields: name, releaseYear, summary, platforms, genres.\n"
                + "Keep summary under 200 characters. Use consistent releaseYear " + year + " only.";
        if (strict) {
            prompt += "\nReturn ONLY valid JSON. No markdown.";
        }
        return prompt;
    }

    private String callOpenAi(String userPrompt) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        String model = openAiProperties.getModel();
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        ChatRequest payload = new ChatRequest();
        payload.setModel(model);
        payload.setTemperature(0.2);
        payload.setMessages(List.of(
                new ChatMessage("system", SYSTEM_PROMPT),
                new ChatMessage("user", userPrompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiProperties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChatRequest> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<ChatResponse> response;
        try {
            logger.info("Calling OpenAI model={} promptLength={}", model, userPrompt.length());
            response = restTemplate.postForEntity(OPENAI_URL, entity, ChatResponse.class);
            logger.info("OpenAI response status={}", response.getStatusCode());
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }
            logger.warn("OpenAI request failed status={} body={}", ex.getStatusCode(), body);
            throw new IllegalStateException("OpenAI request failed", ex);
        } catch (RestClientException ex) {
            logger.warn("OpenAI request failed", ex);
            throw new IllegalStateException("OpenAI request failed", ex);
        }

        ChatResponse body = response.getBody();
        if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI response missing choices");
        }
        ChatMessage message = body.getChoices().get(0).getMessage();
        if (message == null || message.getContent() == null) {
            throw new IllegalStateException("OpenAI response missing content");
        }
        return message.getContent().trim();
    }

    private List<OpenAiGameItem> parseGames(String content, int year, int count) {
        List<OpenAiGameItem> items;
        try {
            items = objectMapper.readValue(content, new TypeReference<List<OpenAiGameItem>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse OpenAI JSON", ex);
        }
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> normalize(item, year))
                .limit(count)
                .collect(Collectors.toList());
    }

    private OpenAiGameItem normalize(OpenAiGameItem item, int year) {
        List<String> platforms = safeList(item.getPlatforms());
        List<String> genres = safeList(item.getGenres());
        return new OpenAiGameItem(
                item.getName(),
                year,
                item.getSummary(),
                platforms,
                genres,
                null
        );
    }

    private List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    @Data
    private static class ChatRequest {
        private String model;
        private Double temperature;
        private List<ChatMessage> messages;
    }

    @Data
    private static class ChatResponse {
        private List<Choice> choices;
    }

    @Data
    private static class Choice {
        private ChatMessage message;
    }

    @Data
    private static class ChatMessage {
        private final String role;
        private final String content;
    }
}
