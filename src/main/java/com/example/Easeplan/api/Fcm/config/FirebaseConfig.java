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
@Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String keyPath;

    @PostConstruct
    public void init() {
        try (InputStream serviceAccount = new FileInputStream(keyPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().stream().noneMatch(app -> app.getName().equals("nogorok"))) {
                FirebaseApp.initializeApp(options, "nogorok");
            }

            // ✅ 여기서 바로 projectId 찍기
            FirebaseApp app = FirebaseApp.getInstance("nogorok");
            System.out.println("Firebase projectId = " + app.getOptions().getProjectId());

        } catch (Exception e) {
            throw new RuntimeException("Firebase 서비스 계정 키 로딩 실패: " + keyPath, e);
        }
    }
}
