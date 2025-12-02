package com.backend.gamelibrarybackend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.admin.credentials.file:}")
    private String firebaseCredentialsFile;

    @Value("${firebase.storage.bucket:}")
    private String firebaseStorageBucket;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        List<FirebaseApp> existingApps = FirebaseApp.getApps();
        if (!existingApps.isEmpty()) {
            return existingApps.get(0);
        }

        GoogleCredentials credentials = loadCredentials();
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials);

        if (StringUtils.hasText(firebaseStorageBucket)) {
            builder.setStorageBucket(firebaseStorageBucket);
        }

        FirebaseOptions options = builder.build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String credentialsPath = resolveCredentialsPath();
        if (!StringUtils.hasText(credentialsPath)) {
            throw new IllegalStateException("Firebase credentials path is not set. Set FIREBASE_CREDENTIALS_FILE or GOOGLE_APPLICATION_CREDENTIALS.");
        }
        try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
            return GoogleCredentials.fromStream(serviceAccount);
        }
    }

    private String resolveCredentialsPath() {
        if (StringUtils.hasText(firebaseCredentialsFile)) {
            return firebaseCredentialsFile;
        }
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(envPath)) {
            return envPath;
        }
        return null;
    }
}
