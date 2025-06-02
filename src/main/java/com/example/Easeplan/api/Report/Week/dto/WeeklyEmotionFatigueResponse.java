package com.example.Easeplan.api.Report.Week.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter
@NoArgsConstructor
public class WeeklyEmotionFatigueResponse {
    private List<DayEmotionFatigueDto> days;

    public WeeklyEmotionFatigueResponse(List<DayEmotionFatigueDto> days) {
        this.days = days;
    }

    // getter, setter
}
