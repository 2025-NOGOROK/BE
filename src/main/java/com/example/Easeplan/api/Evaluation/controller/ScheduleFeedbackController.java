package com.example.Easeplan.api.Evaluation.controller;

import com.example.Easeplan.api.Evaluation.domain.IncompleteReason;
import com.example.Easeplan.api.Evaluation.dto.ScheduleFeedbackRequest;
import com.example.Easeplan.api.Evaluation.service.ScheduleFeedbackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleFeedbackController {

    private final ScheduleFeedbackService service;

    public ScheduleFeedbackController(ScheduleFeedbackService service) {
        this.service = service;
    }

    // 피드백 저장
    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> saveFeedback(@PathVariable Long id, @RequestBody ScheduleFeedbackRequest request) {
        request.setScheduleId(id);
        service.saveFeedback(request);
        return ResponseEntity.ok().build();
    }




    // 미완료 사유별 퍼센트 리포트
    @GetMapping("/report/incomplete-reasons")
    public ResponseEntity<Map<IncompleteReason, Integer>> getIncompleteReasonPercent(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<IncompleteReason, Integer> percentMap = service.getIncompleteReasonPercent(startDateTime, endDateTime);
        return ResponseEntity.ok(percentMap);
    }
}
