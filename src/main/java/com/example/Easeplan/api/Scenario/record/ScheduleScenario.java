package com.example.Easeplan.api.Scenario.record;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;

import java.util.List;

public record ScheduleScenario(
        String type,
        List<FormattedTimeSlot> events,
        List<String> recommendations
) {}