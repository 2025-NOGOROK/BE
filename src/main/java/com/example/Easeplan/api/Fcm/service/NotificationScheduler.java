package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import com.example.Easeplan.api.Fcm.repository.ScheduledNotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
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

    /** 단발성 알림 예약 */
    public void scheduleAlarm(String token, String title, String body, Instant alarmTime) {
        taskScheduler.schedule(() -> {
            boolean ok = fcmService.sendMessage(token, title, body);
            // 실패해도 예외 안 던짐. 필요하면 재시도 로직만 추가
        }, Date.from(alarmTime));
    }

    /** 1분마다 예약 알림 체크 */
    @Scheduled(fixedRate = 60_000)
    public void checkNotifications() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        List<ScheduledNotification> notifications =
                repository.findByNotifyAtBeforeAndIsSentFalse(now);

        notifications.forEach(n -> {
            String msg = n.getTitle() + " 일정이 " + n.getNotifyAt().format(formatter) + "에 시작합니다!";
            try {
                boolean ok = fcmService.sendMessage(
                        n.getFcmToken(),
                        "🔔 " + n.getTitle(),
                        msg
                );
                if (ok) {
                    n.setSent(true);
                } else {
                    n.setRetryCount(n.getRetryCount() + 1);
                    n.setLastError("FCM 전송 실패(unknown)");
                }
            } catch (Exception e) {
                n.setRetryCount(n.getRetryCount() + 1);
                n.setLastError(e.getMessage());
            }
            n.setLastTried(LocalDateTime.now());
            repository.save(n);
        });

    }
}
