package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
public class FcmService {
    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private final UserRepository userRepository;

    // FirebaseApp 인스턴스 이름 (실제 등록한 이름과 일치해야 합니다)
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

    /** 단건 전송: 성공 true / 실패 false (절대 예외 throw X) */
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

    /** 토픽 전송도 실패해도 false만 반환 */
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
            // 토픽은 UNREGISTERED 개념이 없으니 로깅만
            log.warn("FCM topic send failed: {}", e.getMessagingErrorCode(), e);
            return false;

        } catch (Exception e) {
            log.warn("FCM topic unexpected error", e);
            return false;
        }
    }

    /** 공통 예외 처리: UNREGISTERED면 토큰 제거. 절대 예외 재던지지 않음 */
    private void handleFcmException(FirebaseMessagingException e, String token) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED) {
            log.warn("FCM token UNREGISTERED. delete token={}", token);
            removeTokenFromAllUsers(token); // 성능 최적화는 아래 주석 참고
        } else if (code == MessagingErrorCode.INVALID_ARGUMENT) {
            log.warn("FCM invalid argument (token?) token={}", token, e);
        } else if (code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            log.error("FCM senderId mismatch - check project/credentials");
        } else {
            log.warn("FCM send failed: {}", code, e);
        }
    }

    /**
     * ⛔ 현재는 전체 사용자 스캔(O(n)). 트래픽/유저 증가시 성능 이슈.
     * 권장: userRepository에 단건 삭제 쿼리 추가 (예: deleteByFcmToken)
     */
    private void removeTokenFromAllUsers(String invalidToken) {
        userRepository.findAll().forEach(user -> {
            if (user.getFcmTokens().contains(invalidToken)) {
                user.getFcmTokens().remove(invalidToken);
                userRepository.save(user);
            }
        });
    }

    /* 성능 최적화 예시 (UserRepository에 구현 권장)
       @Transactional
       public void removeToken(String token) {
           userRepository.deleteByFcmToken(token);
       }
     */

    /** 비즈니스용 편의 함수 예시 */
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