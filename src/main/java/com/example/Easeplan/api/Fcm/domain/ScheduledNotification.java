package com.example.Easeplan.api.Fcm.domain;

import jakarta.persistence.*;
import lombok.*;
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
}
