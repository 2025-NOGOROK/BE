package com.example.Easeplan.api.Report.Month.controller;

import com.example.Easeplan.api.Report.Month.dto.EmotionPercentResponse;
import com.example.Easeplan.api.Report.Month.dto.DailyStressResponse;
import com.example.Easeplan.api.Report.Month.service.MonthlyReportService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "월간 리포트", description = "월간 감정 비율 및 스트레스 통계 API")
@RequestMapping("/api/monthly")
public class MonthlyReportController {
    private final MonthlyReportService monthlyReportService;
    private final UserRepository userRepository;

    // 감정별 비율
    @Operation(
            summary = "월간 감정별 비율 조회",
            description = """
            사용자의 한 달간 감정 기록을 분석해 감정별 비율(%)을 반환합니다.
            - year,month 안넣으면 현재날짜 기준으로 자동 반환
            - year: 조회 연도 (예: 2025)
            - month: 조회 월 (예: 5)
            ### 반환 예시
            ```
            {
              "emotionPercent": {
                "JOY": 32.0,
                "DEPRESSED": 12.0,
                "NORMAL": 40.0,
                "IRRITATED": 8.0,
                "ANGRY": 8.0
              }
            }
            ```
            """
    )
    @GetMapping("/emotion")
    public ResponseEntity<EmotionPercentResponse> getEmotionPercent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();
        return ResponseEntity.ok(monthlyReportService.getEmotionPercent(user, y, m));
    }


    // 날짜별 스트레스

    @Operation(
            summary = "월간 날짜별 평균 스트레스 및 이모티콘 조회",
            description = """
            사용자의 한 달간 날짜별 평균 스트레스(avg)와 5단계 이모티콘을 반환합니다.
            - year,month 안넣으면 현재날짜 기준으로 자동 반환
            - year: 조회 연도 (예: 2025)
            - month: 조회 월 (예: 5)
            ### 반환 예시
            ```
            {
              "dailyStressList": [
                {"date": "2025-05-01", "avg": 35.0, "emoji": "🙂"},
                {"date": "2025-05-02", "avg": 70.0, "emoji": "😟"},
                ...
              ]
            }
            ```
            """
    )
    @GetMapping("/stress")
    public ResponseEntity<DailyStressResponse> getDailyStress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        return ResponseEntity.ok(monthlyReportService.getDailyStress(user, y, m));
    }

}

