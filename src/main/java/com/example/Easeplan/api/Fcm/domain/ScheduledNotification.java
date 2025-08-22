package com.example.Easeplan.api.Fcm.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class ScheduledNotification {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2048)
    private String fcmToken;

    @Column(nullable = false)
    private ZonedDateTime notifyAt;

    // 필드명은 그대로 isSent 유지 (리포지토리 메서드와 호환)
    @Builder.Default
    private boolean isSent = false;

    @Builder.Default
    private int retryCount = 0;

    private String lastError;
    private LocalDateTime lastTried;

    // ---------- 도메인 메서드 ----------
    public void markSent(LocalDateTime now) {
        this.isSent = true;
        this.lastTried = now;
        this.lastError = null;
    }

    public void markFailed(String errorMessage, LocalDateTime now) {
        this.retryCount += 1;
        this.lastError = errorMessage;
        this.lastTried = now;
    }

    public void markTried(LocalDateTime now) {
        this.lastTried = now;
    }

    public void reschedule(ZonedDateTime nextTime) {
        this.notifyAt = nextTime;
    }

    public void changeTitle(String newTitle) {
        this.title = newTitle;
    }

    public void changeFcmToken(String newToken) {
        this.fcmToken = newToken;
    }
}
