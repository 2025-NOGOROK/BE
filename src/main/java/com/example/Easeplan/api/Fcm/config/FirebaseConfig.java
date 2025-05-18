package com.example.Easeplan.api.Fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() throws IOException {
        // 1. ClassPathResource로 classpath 기준으로 파일 접근 (JAR/로컬 모두 안전)
        ClassPathResource resource = new ClassPathResource(keyPath);

        // 2. 파일 존재 여부 체크
        if (!resource.exists()) {
            throw new NullPointerException("service account is null: " + keyPath);
        }

        // 3. InputStream으로 파일 읽기
        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 4. 중복 초기화 방지
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }
}
