package com.example.Easeplan.api.SmartWatch.controller;

import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import com.example.Easeplan.api.SmartWatch.dto.SmartwatchRequest;
import com.example.Easeplan.api.SmartWatch.service.SmartwatchService;
import com.example.Easeplan.global.auth.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스마트워치 연동", description = "스마트워치 API")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class SmartwatchController {
    private final SmartwatchService smartwatchService; // 필드 추가
    @Operation(
            summary = "스마트워치 데이터 저장",
            description = """
        스마트워치에서 수집한 생체 데이터를 서버에 저장합니다.<br>
        <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
        
        <b>요청 본문 예시:</b>
        <pre>
{
  "email": "user@example.com",
  "timestamp": "2025-05-18T14:30:45",
  "min": 60.0,
  "max": 120.0,
  "avg": 80.0,
  "stress": 75.5,
  "heartRate": 85,
  "startTime": "2025-05-18T14:00:00",
  "endTime": "2025-05-18T14:30:00",
  "totalMinutes": 30,
  "bloodOxygen": 98.2,
  "skinTemperature": 36.5
}
        </pre>
        
        <b>필드 설명:</b>
        - email: 사용자 이메일 <b>[필수]</b><br>
        - timestamp: 측정 시각 (ISO 8601 문자열) <b>[선택, 미입력시 서버시간]</b><br>
        - min, max, avg: 측정값(예: 심박수 등) <b>[선택]</b><br>
        - stress: 스트레스 지수 <b>[선택]</b><br>
        - heartRate: 심박수 <b>[선택]</b><br>
        - startTime, endTime: 측정 구간 <b>[선택]</b><br>
        - totalMinutes: 측정 총 시간(분) <b>[선택]</b><br>
        - bloodOxygen: 혈중 산소 포화도 <b>[선택]</b><br>
        - skinTemperature: 피부 온도 <b>[선택]</b><br>
        
        <b>응답:</b>
        - 200 OK: 데이터 저장 성공
        - 400 Bad Request: 유효성 검사 실패 또는 등록되지 않은 사용자
    """)
    @PostMapping("/data")
    public ResponseEntity<?> submitData(@RequestBody @Valid SmartwatchRequest request) {
        try {
            smartwatchService.saveData(request);
            return ResponseEntity.ok("데이터 저장 성공");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
