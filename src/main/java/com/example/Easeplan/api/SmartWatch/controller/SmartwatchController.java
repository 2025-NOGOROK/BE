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



@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class SmartwatchController {
    private final SmartwatchService smartwatchService;
    private final UserRepository userRepository;
    @Operation(
            summary = "startTime 1시간마다 심박수 데이터를 저장",
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
    @Tag(name = "스마트워치 연동", description = "스마트워치 API")
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
    @Tag(name = "메인페이지", description = "스트레스 관리API+여행추천API")
    @Operation(
            summary = "스트레스 데이터 반환",
            description = """
        헤더에 토큰을 넣어주세요.
        ### 반환 예시
        ```
        {
          "stress": 75.5
        }
        ```
        """)
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestAvg(@AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("GET /api/devices/latest called");
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return smartwatchService.getClosestAvgHeartRate(user)
                .map(avg -> ResponseEntity.ok().body(Map.of("avg", avg)))
                .orElse(ResponseEntity.notFound().build());
    }


}



