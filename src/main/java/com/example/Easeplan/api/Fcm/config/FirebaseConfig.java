package com.example.Easeplan.api.Fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource(keyPath);

        if (!resource.exists()) {
            throw new FileNotFoundException("파일 없음: " + keyPath);
        }

        try (InputStream serviceAccount = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 🔥 명시적 앱 이름 지정 + 중복 초기화 방지
            if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals("easeplan"))) {
                FirebaseApp.initializeApp(options, "easeplan");
            }
        }
    }
}

