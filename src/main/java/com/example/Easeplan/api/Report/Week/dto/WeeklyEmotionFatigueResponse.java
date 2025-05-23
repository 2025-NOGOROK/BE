package com.example.Easeplan.api.Report.Week.dto;

import java.util.List;



public class WeeklyEmotionFatigueResponse {
    private List<DayEmotionFatigueDto> days;

    public WeeklyEmotionFatigueResponse(List<DayEmotionFatigueDto> days) {
        this.days = days;
    }

    // getter, setter
}
