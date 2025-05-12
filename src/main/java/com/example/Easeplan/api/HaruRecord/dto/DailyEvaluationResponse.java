package com.example.Easeplan.api.HaruRecord.dto;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.domain.Emotion;
import com.example.Easeplan.api.HaruRecord.domain.Fatigue;
import com.example.Easeplan.api.HaruRecord.domain.Weather;
import lombok.Getter;

@Getter
public class DailyEvaluationResponse {
    private final String emotion;
    private final String fatigue;
    private final String weather;
    private final Integer sleepHours;
    private final String specialNotes;

    public DailyEvaluationResponse(Emotion emotion, Fatigue fatigue, Weather weather, Integer sleepHours, String specialNotes) {
        this.emotion = emotion.getDescription();
        this.fatigue = fatigue.getDescription();
        this.weather = weather.getDescription();
        this.sleepHours=sleepHours;
        this.specialNotes = specialNotes;
    }

    // 정적 팩토리 메서드 (엔티티 → DTO 변환)
    public static DailyEvaluationResponse from(DailyEvaluation evaluation) {
        return new DailyEvaluationResponse(
                evaluation.getEmotion(),
                evaluation.getFatigueLevel(),
                evaluation.getWeather(),
                evaluation.getSleepHours(),
                evaluation.getSpecialNotes()
        );
    }
}
