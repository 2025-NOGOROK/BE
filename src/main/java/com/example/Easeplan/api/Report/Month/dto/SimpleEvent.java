package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SimpleEvent {
    private String title;
    private String startTime;      // "20:00"
    private String endTime;        // "20:30"
    private String sourceType; // "short-recommend" | "long-recommend" | "emergency"
}
