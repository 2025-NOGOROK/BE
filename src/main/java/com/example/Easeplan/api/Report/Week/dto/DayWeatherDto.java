package com.example.Easeplan.api.Report.Week.dto;

// DayWeatherDto.java
public class DayWeatherDto {
    private String dayOfWeek; // "월", "화", ...
    private String weather;   // 날씨 설명 (null 가능)

    public DayWeatherDto(String dayOfWeek, String weather) {
        this.dayOfWeek = dayOfWeek;
        this.weather = weather;
    }

    // getter, setter
}
