package com.example.Easeplan.api.HaruRecord.controller;

import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationRequest;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationResponse;
import com.example.Easeplan.api.HaruRecord.service.EvaluationService;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public DailyEvaluationResponse getEvaluation(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return evaluationService.getEvaluationByDate(user, date);
    }

    // ★ 추가: 기록 저장 API
    @PostMapping
    public ResponseEntity<?> createEvaluation(
            @AuthenticationPrincipal User user,
            @RequestBody DailyEvaluationRequest request
    ) {
        evaluationService.createEvaluation(user, request);
        return ResponseEntity.ok().build();
    }



}
