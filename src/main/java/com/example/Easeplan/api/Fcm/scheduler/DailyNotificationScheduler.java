package com.example.Easeplan.api.Fcm.scheduler;

import com.example.Easeplan.api.Fcm.service.FcmService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyNotificationScheduler {
    private final FcmService fcmService;

    public DailyNotificationScheduler(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    // 매일 22시(한국시간)에 토픽으로 알림 전송
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void sendDailyNotification() {
        String topic = "daily_10pm";
        String title = "하루 기록 알림";
        String body = "오늘의 기록을 남겨보세요!";

        try {
            fcmService.sendToTopic(topic, title, body);
        } catch (Exception e) {
            // 로깅 또는 예외 처리
            e.printStackTrace();
        }
    }
}
