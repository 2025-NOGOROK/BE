package com.example.Easeplan.api.Report.Month.controller;

import com.example.Easeplan.api.Report.Month.dto.MonthlyReportResponse;
import com.example.Easeplan.api.Report.Month.dto.MonthlyStressReportResponse;
import com.example.Easeplan.api.Report.Month.dto.MonthlyStressTrendResponse;
import com.example.Easeplan.api.Report.Month.service.MonthlyReportService;
import com.example.Easeplan.api.Report.Month.service.MonthlyStressReportService;
import com.example.Easeplan.api.Report.Month.service.MonthlyStressTrendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneId;

@Tag(name = "월간 리포트", description = "월간 쉼표 리포트와 스트레스")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class MonthlyReportController {

    private final MonthlyReportService reportService;
    private final MonthlyStressReportService stressreService;
    private final MonthlyStressTrendService stresstrService;

    @Operation(summary = "월간 쉼표 리포트",
            description = "해당 월의 짧은/긴 쉼표 개수 및 전월 대비 증감을 반환합니다.")
    @GetMapping("/monthly")
    public MonthlyReportResponse getMonthlyReport(   // ← 여기!
                                                     @AuthenticationPrincipal UserDetails user,
                                                     @RequestParam(required = false) Integer year,
                                                     @RequestParam(required = false) Integer month
    ) throws Exception {
        YearMonth ym = (year == null || month == null)
                ? YearMonth.now(ZoneId.of("Asia/Seoul"))
                : YearMonth.of(year, month);

        return reportService.getMonthlyPauseReport(user.getUsername(), ym);
    }


    @Operation(summary = "월간 스트레스 & 쉼표 리포트",
            description = "해당 월의 하루 평균 스트레스가 가장 높았던 날/가장 낮았던 날과, 각 날의 짧은·긴·응급 쉼표 일정을 반환합니다.")
    @GetMapping("/monthly-stress")
    public MonthlyStressReportResponse getMonthlyStress(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) throws Exception {
        YearMonth ym = (year == null || month == null)
                ? YearMonth.now(ZoneId.of("Asia/Seoul"))
                : YearMonth.of(year, month);

        return stressreService.getMonthlyStressReport(user.getUsername(), ym);
    }

    @Operation(
            summary = "최근 월별 스트레스 지수 (최대 3개월)",
            description = "가장 최근 달부터 데이터가 있는 달만 최대 3개월의 월 평균 스트레스를 반환합니다."
    )
    @GetMapping("/monthly-stress-trend")
    public MonthlyStressTrendResponse getTrend(@AuthenticationPrincipal UserDetails user) {
        return stresstrService.getRecentMonthlyStress(user.getUsername());
    }
}
