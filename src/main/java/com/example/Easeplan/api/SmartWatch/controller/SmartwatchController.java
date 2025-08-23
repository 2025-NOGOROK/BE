package com.example.Easeplan.api.SmartWatch.controller;

import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.service.SmartwatchService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
@Tag(name = "스마트워치 연동", description = "스마트워치 API")
public class SmartwatchController {
    private final SmartwatchService smartwatchService;
    private final UserRepository userRepository;

    @Operation(
            summary = "원시 심박 데이터 저장 (단일도 samples에 1개로 보내면 됨)",
            description = """
        ### 요청 예시 (단일)
        {
          "email": "user@example.com",
          "samples": [
            { "timestamp": 1724304000000, "heartRate": 76, "rmssd": 38.2, "stressEma": 41, "stressRaw": 63 }
          ]
        }
        ### 요청 예시 (배치)
        {
          "email": "user@example.com",
          "samples": [
            { "timestamp": 1724304000000, "heartRate": 76, "rmssd": 38.2, "stressEma": 41, "stressRaw": 63 },
            { "timestamp": 1724307600000, "heartRate": 72, "rmssd": 42.1, "stressEma": 35, "stressRaw": 54 }
          ]
        }
        """
    )
    @PostMapping("/heartrate")
    public ResponseEntity<?> submitRawData(@RequestBody HeartRateRequest request) {
        if (request.getEmail() == null) {
            return ResponseEntity.badRequest().body("이메일은 필수입니다.");
        }
        smartwatchService.saveData(request);
        return ResponseEntity.ok("원시 데이터 저장 성공");
    }

    @Operation(
            summary = "가장 최신 stressEma 반환",
            description = """
        헤더에 토큰을 넣어주세요.
        ### 응답 예시
        { "stressEma": 41.0 }
        """
    )
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return smartwatchService.getLatestStressEma(user)
                .map(val -> ResponseEntity.ok().body(Map.of("stressEma", val)))
                .orElse(ResponseEntity.notFound().build());
    }
}
