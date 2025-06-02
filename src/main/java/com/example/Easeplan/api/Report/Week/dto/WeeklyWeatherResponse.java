package com.example.Easeplan.api.Report.Week.dto;

// WeeklyWeatherResponse.java
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class WeeklyWeatherResponse {
    private List<DayWeatherDto> days;

    public WeeklyWeatherResponse(List<DayWeatherDto> days) {
        this.days = days;
    }

    // getter, setter
}
