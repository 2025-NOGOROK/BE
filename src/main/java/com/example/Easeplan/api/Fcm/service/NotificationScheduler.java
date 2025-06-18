package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import com.example.Easeplan.api.Fcm.repository.ScheduledNotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
public class NotificationScheduler {
    private final TaskScheduler taskScheduler;
    private final FcmService fcmService;
    private final ScheduledNotificationRepository repository;

    // DateTimeFormatter (yyyy-MM-dd HH:mm 형식)
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NotificationScheduler(
            @Qualifier("taskScheduler") TaskScheduler taskScheduler,
            FcmService fcmService,
            ScheduledNotificationRepository repository
    ) {
        this.taskScheduler = taskScheduler;
        this.fcmService = fcmService;
        this.repository = repository;
    }

    // 단발성 알림 예약 (직접 시간 지정)
    public void scheduleAlarm(String token, String title, String body, Instant alarmTime) {
        taskScheduler.schedule(() -> {
            try {
                fcmService.sendMessage(token, title, body);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Date.from(alarmTime));
    }

    // 1분마다 예약된 알림 체크 (DB 기반)
    @Scheduled(fixedRate = 60_000)
    public void checkNotifications() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        // 현재 시간 이전에 예약된 미발송 알림 조회
        List<ScheduledNotification> notifications =
                repository.findByNotifyAtBeforeAndIsSentFalse(now);

        notifications.forEach(notification -> {
            try {
                fcmService.sendMessage(
                        notification.getFcmToken(),
                        "🔔 " + notification.getTitle(),
                        notification.getTitle() + " 일정이 " +
                                notification.getNotifyAt().format(formatter) + "에 시작합니다!"
                );
                notification.setSent(true);
                repository.save(notification);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }); }
}
