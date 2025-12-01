package com.backend.gamelibrarybackend.service;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;

@Service
public class FirebaseStorageService {

    @Value("${firebase.storage.bucket:}")
    private String storageBucket;

    public String upload(MultipartFile file, String userId) throws IOException {
        Bucket bucket = StorageClient.getInstance().bucket(resolveBucketName());
        if (bucket == null) {
            throw new IllegalStateException("Firebase storage bucket is not configured");
        }

        String originalName = file.getOriginalFilename();
        String safeName = (originalName != null) ? Paths.get(originalName).getFileName().toString() : "upload";
        String objectName = userId + "/" + System.currentTimeMillis() + "_" + safeName;

        Blob blob = bucket.create(objectName, file.getInputStream(), file.getContentType());
        // Make the uploaded object publicly readable so returned URL works without signed URLs.
        blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

        return String.format("https://storage.googleapis.com/%s/%s", bucket.getName(), objectName);
    }

    private String resolveBucketName() {
        if (storageBucket != null && !storageBucket.isBlank()) {
            return storageBucket;
        }
        // Fallback to default bucket configured in FirebaseOptions
        return StorageClient.getInstance().bucket().getName();
    }
}
