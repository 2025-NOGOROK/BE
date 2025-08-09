package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyStressReportResponse {
    private int year;
    private int month;

    private StressDayReport mostStressful; // 최댓값
    private StressDayReport leastStressful; // 최솟값
}
