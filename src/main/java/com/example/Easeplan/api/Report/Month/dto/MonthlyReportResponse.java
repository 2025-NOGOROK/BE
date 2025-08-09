package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyReportResponse {
    private int year;
    private int month;

    private int shortCount;   // 이달 짧은 쉼표 개수
    private int longCount;    // 이달 긴 쉼표 개수

    private int shortDiffFromPrev; // 전월 대비 증감(+, -)
    private int longDiffFromPrev;  // 전월 대비 증감
}
