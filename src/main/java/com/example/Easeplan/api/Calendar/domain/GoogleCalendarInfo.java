package com.example.Easeplan.api.Calendar.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "google_calendar_info")
public class GoogleCalendarInfo {
    @Id
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email")
    private User user;

    private String calendarId; // 추가 (기본값 "primary")
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // 새로 추가된 필드
    private boolean serverAlarm;
    private int minutesBeforeAlarm;
    private boolean fixed;
    private boolean userLabel;


}

