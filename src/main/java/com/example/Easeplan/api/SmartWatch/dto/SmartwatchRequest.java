package com.example.Easeplan.api.SmartWatch.dto;

import lombok.Builder;

@Builder
public record SmartwatchRequest(
        String email,

        String timestamp,   // 측정 시각 (String 또는 LocalDateTime)
        Float min,
        Float max,
        Float avg,
        Float stress,
        Integer heartRate,
        String startTime,
        String endTime,
        Integer totalMinutes,
        Float bloodOxygen,
        Float skinTemperature
) {}
