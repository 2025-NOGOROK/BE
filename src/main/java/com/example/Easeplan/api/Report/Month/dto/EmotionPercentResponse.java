package com.example.Easeplan.api.Report.Month.dto;

import java.util.Map;

public class EmotionPercentResponse {
    private Map<String, Double> emotionPercent; // 예: {"JOY": 32.0, "DEPRESSED": 12.0, ...}

    public EmotionPercentResponse(Map<String, Double> emotionPercent) {
        this.emotionPercent = emotionPercent;
    }

    public Map<String, Double> getEmotionPercent() {
        return emotionPercent;
    }
}
