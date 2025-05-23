package com.example.Easeplan.api.Report.Week.dto;

public class DayEmotionFatigueDto {
    private String dayOfWeek; // "월", "화", ...
    private String emotion;   // 감정 설명 (null 가능)
    private String fatigue;   // 피로도 설명 (null 가능)
    // 생성자, getter/setter
    public DayEmotionFatigueDto(String dayOfWeek, String emotion, String fatigue) {
        this.dayOfWeek = dayOfWeek;
        this.emotion = emotion;
        this.fatigue = fatigue;
    }
}
