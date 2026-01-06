package com.backend.gamelibrarybackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.file.Paths;
import java.time.Duration;

@Service
@Profile("!local")
public class S3PresignService {

    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;

    public S3PresignService(S3Presigner presigner,
                            @Value("${storage.s3.bucket:}") String bucket,
                            @Value("${storage.s3.public-base-url:}") String publicBaseUrl) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    public PresignResult presignPut(String userId, String filename, String contentType, Duration expiresIn) {
        String safeName = (filename != null && !filename.isBlank())
                ? Paths.get(filename).getFileName().toString()
                : "upload";
        String key = userId + "/" + System.currentTimeMillis() + "_" + safeName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiresIn)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return new PresignResult(presigned.url().toString(), key, buildPublicUrl(key), expiresIn.getSeconds());
    }

    private String buildPublicUrl(String key) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return null;
        }
        String separator = publicBaseUrl.endsWith("/") ? "" : "/";
        return publicBaseUrl + separator + key;
    }

    public record PresignResult(String url, String key, String publicUrl, long expiresInSeconds) {}
}
