package com.backend.gamelibrarybackend.dto;

public class S3PresignResponse {
    private final String url;
    private final String key;
    private final String publicUrl;
    private final long expiresInSeconds;

    public S3PresignResponse(String url, String key, String publicUrl, long expiresInSeconds) {
        this.url = url;
        this.key = key;
        this.publicUrl = publicUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
