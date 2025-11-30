package com.example.Easeplan.api.HaruRecord.controller;

import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationRequest;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationResponse;
import com.example.Easeplan.api.HaruRecord.service.EvaluationService;
import com.example.Easeplan.global.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "하루기록", description = "하루기록 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/haru")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }
    @Operation(summary = "하루기록 조회", description = """
            일정에 대한 하루기록을 조회합니다.
            """)
    @GetMapping
    public DailyEvaluationResponse getEvaluation(

            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return evaluationService.getEvaluationByDate(user, date);
    }



    // ★ 추가: 기록 저장 API
    @Operation(
            summary = "하루기록 저장",
            description = """
        사용자가 하루 동안 느낀 감정, 피로도, 날씨, 수면시간, 특이사항 등을 저장합니다.
        
        - 모든 필드는 필수이며, 감정/피로도/날씨는 아래 Enum 값 중 하나여야 합니다.
        - 날짜는 `yyyy-MM-dd` 형식으로 전달해야 합니다.
        - 인증이 필요합니다(AccessToken).
        
        **요청 예시**
        ```
        {
          "date": "2025-05-23",
          "emotion": "JOY",
          "fatigueLevel": "NORMAL",
          "weather": "SUNNY",
          "sleepHours": 7,
          "specialNotes": "오늘은 기분 좋은 하루였어요."
        }
        ```
        """
    )

    @PostMapping
    public ResponseEntity<?> createEvaluation(

            @AuthenticationPrincipal User user,
            @RequestBody DailyEvaluationRequest request
    ) {
        evaluationService.createEvaluation(user, request);
        return ResponseEntity.ok().build();
    }



}
