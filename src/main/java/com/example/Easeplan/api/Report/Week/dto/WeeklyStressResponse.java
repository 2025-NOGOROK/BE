package com.example.Easeplan.api.Report.Week.dto;

import java.util.List;

public class WeeklyStressResponse {
    private List<DayStressDto> days;

    public WeeklyStressResponse(List<DayStressDto> days) {
        this.days = days;
    }

    // getter, setter
}