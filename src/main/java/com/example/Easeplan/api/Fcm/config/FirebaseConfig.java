package com.example.Easeplan.api.Fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() {
        // ✅ 테스트 환경이면 Firebase 초기화 생략
        String activeProfile = System.getProperty("spring.profiles.active", "");
        if (activeProfile.contains("test") || isTestEnvironment()) {
            System.out.println("[FirebaseConfig] 테스트 환경이므로 Firebase 초기화를 생략합니다.");
            return;
        }

        try (InputStream serviceAccount = new FileInputStream(keyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals("easeplan"))) {
                FirebaseApp.initializeApp(options, "easeplan");
            }
        } catch (Exception e) {
            throw new RuntimeException("Firebase 서비스 계정 키 로딩 실패: " + keyPath, e);
        }
    }

    private boolean isTestEnvironment() {
        // JUnit 환경이면 테스트로 간주
        return Thread.currentThread().getStackTrace().toString().contains("org.junit");
    }
}
