package com.example.Easeplan.api.Fcm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
@Getter
@Setter

@Entity
public class ScheduledNotification {
    @Id
    @GeneratedValue
    private Long id;
    private String title;          // 일정 제목
    private String fcmToken;       // 사용자 FCM 토큰
    private ZonedDateTime notifyAt; // 알림 보낼 시간 (KST)
    private boolean isSent = false; // 발송 여부
}
