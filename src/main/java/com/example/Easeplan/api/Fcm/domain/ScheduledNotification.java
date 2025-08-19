package com.example.Easeplan.api.Fcm.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledNotification {
    @Id @GeneratedValue
    private Long id;

    private String title;
    private String fcmToken;
    private ZonedDateTime notifyAt;
    private boolean isSent = false;

    private int retryCount;          // 몇 번 시도했는지
    private String lastError;        // 마지막 에러 메시지
    private LocalDateTime lastTried; // 마지막 시도 시간
}
