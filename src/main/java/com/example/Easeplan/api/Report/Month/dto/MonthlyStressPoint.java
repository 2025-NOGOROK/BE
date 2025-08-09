package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyStressPoint {
    private int year;      // 예: 2025
    private int month;     // 1~12
    private int value;     // 차트 표시에 맞춰 반올림 정수 (예: 75, 85, 100)
}