package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class StressDayReport {
    private LocalDate date;
    private double avgStress;

    private int shortCount;
    private int longCount;
    private int emergencyCount;

    private List<SimpleEvent> shortEvents;
    private List<SimpleEvent> longEvents;
    private List<SimpleEvent> emergencyEvents;
}
