package com.example.Easeplan.api.Report.Week.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class WeeklyStressResponse {
    private List<DayStressDto> days;

    public WeeklyStressResponse(List<DayStressDto> days) {
        this.days = days;
    }

    // getter, setter
}