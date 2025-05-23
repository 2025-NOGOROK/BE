package com.example.Easeplan.api.Report.Week.controller;

import com.example.Easeplan.api.Report.Week.dto.WeeklyEmotionFatigueResponse;
import com.example.Easeplan.api.Report.Week.dto.WeeklyStressResponse;
import com.example.Easeplan.api.Report.Week.dto.WeeklyWeatherResponse;
import com.example.Easeplan.api.Report.Week.service.WeeklyService;
import com.example.Easeplan.global.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "주간리포트", description = "평균 스트레스+감정/피로도+날씨")
@RestController
public class WeekController {

    private final WeeklyService weeklyService;

    public WeekController(WeeklyService weeklyService) {
        this.weeklyService = weeklyService;
    }

    @Operation(
            summary = "주간 평균 스트레스 통계 조회",
            description = """
            토큰을 넣어주세요.
            오늘 날짜가 속한 주(월요일~일요일)의 각 요일별로 스트레스 평균을 반환합니다.
            각 요일에 기록이 없으면 값은 null로 표시됩니다.
            반환 데이터는 월~일 순서로 정렬되어 있습니다.
        """
    )
    @GetMapping("/api/weekly/stress")
    public WeeklyStressResponse getCurrentWeekStress(
            @AuthenticationPrincipal User user
    ) {
        return weeklyService.getCurrentWeekStress(user);
    }

    @Operation(
            summary = "주간 감정/피로도 통계 조회",
            description = """
            토큰을 넣어주세요.
            오늘 날짜가 속한 주(월요일~일요일)의 각 요일별로 감정 및 피로도 기록을 반환합니다.
            각 요일에 기록이 없으면 값은 null로 표시됩니다.
            반환 데이터는 월~일 순서로 정렬되어 있습니다.
        """
    )
    @GetMapping("/api/weekly/emotion-fatigue")
    public WeeklyEmotionFatigueResponse getCurrentWeekEmotionFatigue(
            @AuthenticationPrincipal User user
    ) {
        return weeklyService.getCurrentWeekEmotionFatigue(user);
    }

    @Operation(
            summary = "주간 날씨 통계 조회",
            description = """
            토큰을 넣어주세요.
            오늘 날짜가 속한 주(월요일~일요일)의 각 요일별로 날씨 기록을 반환합니다.
            각 요일에 기록이 없으면 값은 null로 표시됩니다.
            반환 데이터는 월~일 순서로 정렬되어 있습니다.
        """
    )
    @GetMapping("/api/weekly/weather")
    public WeeklyWeatherResponse getCurrentWeekWeather(
            @AuthenticationPrincipal User user
    ) {
        return weeklyService.getCurrentWeekWeather(user);
    }
}
