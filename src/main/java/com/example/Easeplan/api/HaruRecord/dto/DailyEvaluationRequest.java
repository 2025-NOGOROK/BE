package com.example.Easeplan.api.HaruRecord.dto;

import com.example.Easeplan.api.HaruRecord.domain.Emotion;
import com.example.Easeplan.api.HaruRecord.domain.Fatigue;
import com.example.Easeplan.api.HaruRecord.domain.Weather;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyEvaluationRequest {
    private LocalDate date;
    private Emotion emotion;
    private Fatigue fatigueLevel;
    private Weather weather;
    private String specialNotes;
}
