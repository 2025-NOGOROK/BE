package com.example.Easeplan.api.Report.Month.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MonthlyStressTrendResponse {
    // 시간순(오래된→최근)으로 정렬된 포인트들, 최대 3개
    private List<MonthlyStressPoint> points;
}
