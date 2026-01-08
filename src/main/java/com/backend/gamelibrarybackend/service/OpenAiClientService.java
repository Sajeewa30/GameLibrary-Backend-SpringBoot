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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OpenAiClientService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT = "You are a video game historian. Return only valid JSON. No extra text.";
    private static final Logger logger = LoggerFactory.getLogger(OpenAiClientService.class);
    private static final int CACHE_COUNT = 100;
    private static final int MAX_RETRIES = 2;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();

    public OpenAiClientService(ObjectMapper objectMapper,
                               OpenAiProperties openAiProperties,
                               RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(20_000);
                    factory.setReadTimeout(120_000);
                    return factory;
                })
                .build();
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
    }

    public List<OpenAiGameItem> fetchGames(int year, int count) {
        CacheEntry cached = cache.get(year);
        if (cached != null) {
            return trimToCount(cached.items, count);
        }

        String userPrompt = buildUserPrompt(year, CACHE_COUNT, false, true);
        try {
            List<OpenAiGameItem> items = parseGames(callOpenAi(userPrompt), year, CACHE_COUNT);
            if (items.isEmpty()) {
                String relaxedPrompt = buildUserPrompt(year, CACHE_COUNT, false, false);
                items = parseGames(callOpenAi(relaxedPrompt), year, CACHE_COUNT);
            }
            cache.put(year, new CacheEntry(items));
            return trimToCount(items, count);
        } catch (IllegalStateException ex) {
            String strictPrompt = buildUserPrompt(year, CACHE_COUNT, true, true);
            List<OpenAiGameItem> items = parseGames(callOpenAi(strictPrompt), year, CACHE_COUNT);
            if (items.isEmpty()) {
                String relaxedStrictPrompt = buildUserPrompt(year, CACHE_COUNT, true, false);
                items = parseGames(callOpenAi(relaxedStrictPrompt), year, CACHE_COUNT);
            }
            cache.put(year, new CacheEntry(items));
            return trimToCount(items, count);
        }
    }

    private String buildUserPrompt(int year, int count, boolean strict, boolean requireAllPlatforms) {
        String prompt = "List the top " + count + " most popular and notable video games released in " + year + ".\n"
                + "Only include games that actually released in " + year + " (do not guess or invent).\n"
                + "Exclude Nintendo-only games.\n"
                + (requireAllPlatforms
                ? "Only include games available on PC, PlayStation, and Xbox (all three).\n"
                : "Only include games available on PC, PlayStation, or Xbox (at least one).\n")
                + "Exclude mobile-only games (multi-platform games that include mobile are OK).\n"
                + "Return JSON array items with fields: name, releaseYear, summary, platforms, genres.\n"
                + "Keep summary under 50 characters and make it a short description of the game.\n"
                + "Use consistent releaseYear " + year + " only.";
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
            model = "gpt-5.2";
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
        ResponseEntity<ChatResponse> response = null;
        int attempt = 0;
        while (true) {
            try {
                logger.info("Calling OpenAI model={} promptLength={} attempt={}", model, userPrompt.length(), attempt + 1);
                response = restTemplate.postForEntity(OPENAI_URL, entity, ChatResponse.class);
                logger.info("OpenAI response status={}", response.getStatusCode());
                break;
            } catch (HttpStatusCodeException ex) {
                if (ex.getStatusCode().is5xxServerError() && attempt < MAX_RETRIES) {
                    logger.warn("OpenAI 5xx response, retrying attempt={}", attempt + 2);
                    sleepBackoff(++attempt);
                    continue;
                }
                String body = ex.getResponseBodyAsString();
                if (body != null && body.length() > 500) {
                    body = body.substring(0, 500) + "...";
                }
                logger.warn("OpenAI request failed status={} body={}", ex.getStatusCode(), body);
                throw new IllegalStateException("OpenAI request failed", ex);
            } catch (RestClientException ex) {
                if (isRetryable(ex) && attempt < MAX_RETRIES) {
                    logger.warn("OpenAI request timed out, retrying attempt={}", attempt + 2);
                    sleepBackoff(++attempt);
                    continue;
                }
                logger.warn("OpenAI request failed", ex);
                throw new IllegalStateException("OpenAI request failed", ex);
            }
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

    private List<OpenAiGameItem> trimToCount(List<OpenAiGameItem> items, int count) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
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

    private boolean isRetryable(RestClientException ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        Throwable cause = ex.getCause();
        return cause instanceof java.net.SocketTimeoutException;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(500L * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class CacheEntry {
        private final List<OpenAiGameItem> items;

        private CacheEntry(List<OpenAiGameItem> items) {
            this.items = items;
        }
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
