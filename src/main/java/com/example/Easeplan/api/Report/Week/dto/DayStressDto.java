package com.example.Easeplan.api.Report.Week.dto;

public class DayStressDto {
    private String dayOfWeek; // "월", "화", ...
    private Float averageStress; // null 가능

    public DayStressDto(String dayOfWeek, Float averageStress) {
        this.dayOfWeek = dayOfWeek;
        this.averageStress = averageStress;
    }

    // getter, setter
}