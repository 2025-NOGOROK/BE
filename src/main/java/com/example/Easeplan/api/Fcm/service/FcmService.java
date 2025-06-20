package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    private final UserRepository userRepository;

    public FcmService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String sendMessage(String token, String title, String body) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        FirebaseApp app = FirebaseApp.getInstance("nogorok");

        try {
            return FirebaseMessaging.getInstance(app).send(message);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                removeTokenFromAllUsers(token); // ⬅️ 여기서 DB에서 삭제
            }
            throw e;
        }
    }

    public String sendToTopic(String topic, String title, String body) throws Exception {
        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        FirebaseApp app = FirebaseApp.getInstance("nogorok");
        return FirebaseMessaging.getInstance(app).send(message);
    }

    // ⛔ 유효하지 않은 FCM 토큰 삭제
    private void removeTokenFromAllUsers(String invalidToken) {
        userRepository.findAll().forEach(user -> {
            if (user.getFcmTokens().contains(invalidToken)) {
                user.getFcmTokens().remove(invalidToken);
                userRepository.save(user);
            }
        });
    }
}
