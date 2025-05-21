package com.example.Easeplan.api.SmartWatch.controller;

import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.service.SmartwatchService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Tag(name = "스마트워치 연동", description = "스마트워치 API")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class SmartwatchController {
    private final SmartwatchService smartwatchService;
    private final UserRepository userRepository;
    @Operation(
            summary = "심박수 데이터 저장/수정",
            description = """
        ### 저장 예시
        ```
        {
          "email": "user@example.com",
          "min": 60.0,
          "max": 120.0,
          "avg": 80.0,
          "startTime": "2025-05-18T14:00:00",
          "endTime": "2025-05-18T14:30:00",
          "count": 150,
          "stress": 75.5
        }
        ```
        """)
    @PostMapping("/heartrate")
    public ResponseEntity<?> submitData(@RequestBody @Valid HeartRateRequest request) {
        try {
            if (request.getEmail() == null) {
                throw new RuntimeException("이메일은 필수입니다.");
            }
            smartwatchService.saveData(request);
            return ResponseEntity.ok("데이터 저장 성공");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 최근 스트레스 지수 반환
    @Operation(
            summary = "심박수 데이터 반환",
            description = """
        ### 반환 예시
        ```
        {
          "email": "user@example.com",
          "min": 60.0,
          "max": 120.0,
          "avg": 80.0,
          "startTime": "2025-05-18T14:00:00",
          "endTime": "2025-05-18T14:30:00",
          "count": 150,
          "stress": 75.5
        }
        ```
        """)
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestStress(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return smartwatchService.getLatestHeartRate(user)
                .map(hr -> ResponseEntity.ok().body(Map.of(
                        "stress", hr.getStress(),
                        "avg", hr.getAvg(),
                        "min", hr.getMin(),
                        "max", hr.getMax(),
                        "startTime", hr.getStartTime(),
                        "endTime", hr.getEndTime()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}



