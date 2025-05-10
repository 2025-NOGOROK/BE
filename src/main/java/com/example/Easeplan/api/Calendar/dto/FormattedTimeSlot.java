package com.example.Easeplan.api.Calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FormattedTimeSlot {
    private final String startTime;
    private final String endTime;
}