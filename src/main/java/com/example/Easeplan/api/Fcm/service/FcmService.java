package com.example.Easeplan.api.Fcm.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public String sendMessage(String token, String title, String body) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        // ❗ FirebaseApp.getInstance("nogorok")는 여기서 호출해야 초기화 타이밍 문제 안 생김
        FirebaseApp app = FirebaseApp.getInstance("nogorok");
        return FirebaseMessaging.getInstance(app).send(message);
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
}
