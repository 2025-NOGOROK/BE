package com.example.Easeplan.api.Fcm.service;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import com.example.Easeplan.api.Fcm.repository.ScheduledNotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
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
            // 실패 시 별도 재시도/로그 필요하면 이쪽에서 처리
        }, Date.from(alarmTime));
    }

    /** 1분마다 예약 알림 체크 */
    @Scheduled(fixedRate = 60_000)
    public void checkNotifications() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        List<ScheduledNotification> notifications =
                repository.findByNotifyAtBeforeAndIsSentFalse(now);

        for (ScheduledNotification n : notifications) {
            String msg = n.getTitle() + " 일정이 " + n.getNotifyAt().format(formatter) + "에 시작합니다!";
            try {
                boolean ok = fcmService.sendMessage(
                        n.getFcmToken(),
                        "🔔 " + n.getTitle(),
                        msg
                );
                if (ok) {
                    n.markSent(LocalDateTime.now());
                } else {
                    n.markFailed("FCM 전송 실패(unknown)", LocalDateTime.now());
                }
            } catch (Exception e) {
                n.markFailed(e.getMessage(), LocalDateTime.now());
            }
            repository.save(n); // 더티체킹으로도 되지만 명시 save 유지
        }
    }
}
