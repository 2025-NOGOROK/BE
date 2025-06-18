package com.example.Easeplan.api.Fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile; // ⬅️ 추가

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
@Profile("!test") // ⬅️ 테스트 환경에서는 이 설정 파일 자체를 무시!
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() {
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
}
