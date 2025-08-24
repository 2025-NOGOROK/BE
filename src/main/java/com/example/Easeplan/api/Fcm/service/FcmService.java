package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FcmService {
    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final UserRepository userRepository;

    // FirebaseApp 인스턴스 이름 (FirebaseConfig 초기화명과 일치)
    private static final String FIREBASE_APP_NAME = "nogorok";

    public FcmService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void logProjectId() {
        try {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            log.info("Firebase projectId={}", app.getOptions().getProjectId());
        } catch (IllegalStateException e) {
            log.error("FirebaseApp '{}' not found. Check initialization.", FIREBASE_APP_NAME, e);
        }
    }

    /** 단건 전송: 성공 true / 실패 false (예외 throw 안 함) */
    public boolean sendMessage(String token, String title, String body) {
        if (token == null || token.isBlank()) {
            log.debug("Skip FCM: empty token");
            return false;
        }
        try {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build();

            String id = FirebaseMessaging.getInstance(app).send(message);
            log.debug("FCM sent id={}", id);
            return true;

        } catch (FirebaseMessagingException e) {
            handleFcmException(e, token);
            return false;
        } catch (Exception e) {
            log.warn("FCM unexpected error", e);
            return false;
        }
    }

    /** 데이터 포함 버전 */
    public boolean sendMessage(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isBlank()) {
            log.debug("Skip FCM: empty token");
            return false;
        }
        try {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            Message.Builder b = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build());
            if (data != null && !data.isEmpty()) b.putAllData(data);

            String id = FirebaseMessaging.getInstance(app).send(b.build());
            log.debug("FCM sent id={}", id);
            return true;

        } catch (FirebaseMessagingException e) {
            handleFcmException(e, token);
            return false;
        } catch (Exception e) {
            log.warn("FCM unexpected error", e);
            return false;
        }
    }

    /** 토픽 전송 */
    public boolean sendToTopic(String topic, String title, String body) {
        try {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);
            Message msg = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build();
            String id = FirebaseMessaging.getInstance(app).send(msg);
            log.debug("FCM topic sent id={} topic={}", id, topic);
            return true;

        } catch (FirebaseMessagingException e) {
            log.warn("FCM topic send failed: {}", e.getMessagingErrorCode(), e);
            return false;

        } catch (Exception e) {
            log.warn("FCM topic unexpected error", e);
            return false;
        }
    }

    /** 공통 예외 처리: UNREGISTERED면 토큰 제거 */
    private void handleFcmException(FirebaseMessagingException e, String token) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED) {
            log.warn("FCM token UNREGISTERED. delete token={}", token);
            removeTokenFromAllUsers(token);
        } else if (code == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("FCM invalid argument (token?) token={}", token, e);
        } else if (code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            log.error("FCM senderId mismatch - check project/credentials");
        } else {
            log.warn("FCM send failed: {}", code, e);
        }
    }

    /** ⛔ O(n) 삭제. 유저 증가 시 repo 단건 삭제 쿼리 권장 */
    private void removeTokenFromAllUsers(String invalidToken) {
        userRepository.findAll().forEach(user -> {
            if (user.getFcmTokens().contains(invalidToken)) {
                user.getFcmTokens().remove(invalidToken);
                userRepository.save(user);
            }
        });
    }

    /** (신규) 토큰 유효성 검증: dry-run data-only 전송 */
    public Map<String, Object> validateToken(String token) {
        return sendMessageDebug(token, null, null, Map.of("type", "VALIDATE"), true);
    }

    /** 디버그/검증 공용: dryRun 지원 + 에러 디테일 반환 */
    public Map<String, Object> sendMessageDebug(String token, String title, String body,
                                                Map<String, String> data, boolean dryRun) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            FirebaseApp app = FirebaseApp.getInstance(FIREBASE_APP_NAME);

            Message.Builder b = Message.builder().setToken(token);
            if ((title != null && !title.isBlank()) || (body != null && !body.isBlank())) {
                b.setNotification(Notification.builder()
                        .setTitle(title == null ? "" : title)
                        .setBody(body == null ? "" : body)
                        .build());
            }
            if (data != null && !data.isEmpty()) b.putAllData(data);

            String id = FirebaseMessaging.getInstance(app).send(b.build(), dryRun);
            res.put("ok", true);
            res.put("dryRun", dryRun);
            res.put("messageId", id);
            return res;

        } catch (FirebaseMessagingException e) {
            res.put("ok", false);
            res.put("dryRun", dryRun);
            if (e.getMessagingErrorCode() != null) res.put("code", e.getMessagingErrorCode().name());
            if (e.getErrorCode() != null)          res.put("errorCode", e.getErrorCode());
            res.put("msg", e.getMessage());

            try {
                var http = e.getHttpResponse(); // IncomingHttpResponse
                if (http != null) {
                    res.put("httpStatus", http.getStatusCode());
                    try {
                        Object bodyObj = http.getContent(); // byte[] 또는 String
                        if (bodyObj != null) {
                            if (bodyObj instanceof byte[]) {
                                res.put("httpBody", new String((byte[]) bodyObj, StandardCharsets.UTF_8));
                            } else {
                                res.put("httpBody", bodyObj.toString());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            return res;

        } catch (Exception e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", false);
            out.put("msg", e.toString());
            return out;
        }
    }

    /** 비즈니스용 편의 함수 */
    public boolean sendEmergencyStressAsk(String token, Long eventId) {
        return sendMessage(
                token,
                "⚠ 만성 스트레스 신호 감지",
                "최근 14일 지표에서 고스트레스가 감지됐어요. ‘활성화’를 눌러 3일 간 짧은 쉼표를 자동 배정해요.",
                Map.of(
                        "type", "EMERGENCY_STRESS",
                        "eventId", String.valueOf(eventId),
                        "deeplink", "easeplan://emergency/activate?eventId=" + eventId
                )
        );
    }
}
