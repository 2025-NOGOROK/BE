package com.example.Easeplan.api.Fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() throws IOException {
        try (InputStream serviceAccount = new FileInputStream(keyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals("easeplan"))) {
                FirebaseApp.initializeApp(options, "easeplan");
            }
        } catch (IOException e) {
            throw new RuntimeException("Firebase 서비스 계정 키 로딩 실패: " + keyPath, e);
        }
    }
}

