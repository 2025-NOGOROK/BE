package com.example.Easeplan.api.Fcm.repository;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.ZonedDateTime;
import java.util.List;

public interface ScheduledNotificationRepository
        extends JpaRepository<ScheduledNotification, Long> {
    List<ScheduledNotification> findByNotifyAtBeforeAndIsSentFalse(ZonedDateTime time);

    // 알림 시간이 지정 시간 이전이고 미발송된 알림 조회
}
