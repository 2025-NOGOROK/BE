package com.example.Easeplan.api.Report.Week.dto;

// WeeklyWeatherResponse.java
import java.util.List;

public class WeeklyWeatherResponse {
    private List<DayWeatherDto> days;

    public WeeklyWeatherResponse(List<DayWeatherDto> days) {
        this.days = days;
    }

    // getter, setter
}
