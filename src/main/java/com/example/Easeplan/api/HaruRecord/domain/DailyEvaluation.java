package com.example.Easeplan.api.HaruRecord.domain;

import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationRequest;
import com.example.Easeplan.global.auth.domain.BaseEntity;
import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_evaluation")
public class DailyEvaluation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Emotion emotion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fatigue fatigueLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Weather weather;

    @Column(name = "sleep_hours")
    private Integer sleepHours;

    @Column(length = 500)
    private String specialNotes;

    public void updateFrom(DailyEvaluationRequest req) {
        this.emotion = req.getEmotion();
        this.fatigueLevel = req.getFatigueLevel();
        this.weather = req.getWeather();
        this.specialNotes = req.getSpecialNotes();
        // googleEventId 등 유지/변경 필요 시 여기서 처리
    }
}
