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

    // 매일 22시(한국) 토픽 전송 (실패해도 예외 던지지 않음)
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void sendDailyNotification() {
        fcmService.sendToTopic("daily_10pm", "하루 기록 알림", "오늘의 기록을 남겨보세요!");
    }
}