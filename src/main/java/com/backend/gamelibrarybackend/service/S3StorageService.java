package com.backend.gamelibrarybackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Paths;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(S3Client s3Client,
                            @Value("${storage.s3.bucket}") String bucket,
                            @Value("${storage.s3.public-base-url}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    public String upload(MultipartFile file, String userId) throws IOException {
        String originalName = file.getOriginalFilename();
        String safeName = (originalName != null) ? Paths.get(originalName).getFileName().toString() : "upload";
        String key = userId + "/" + System.currentTimeMillis() + "_" + safeName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload to object storage", e);
        }

        String separator = publicBaseUrl.endsWith("/") ? "" : "/";
        return publicBaseUrl + separator + key;
    }
}
