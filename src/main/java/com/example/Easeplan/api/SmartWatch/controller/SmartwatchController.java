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

    private final SmartwatchService smartwatchService;

    // 갤럭시 워치 등록
// SmartwatchController.java
    @Operation(
            summary = "스마트워치 데이터 전송",
            description = """
        스마트워치에서 수집한 생체 데이터를 서버에 저장합니다.<br>
        <b>인증 없이 호출 가능하며, 기기 등록 후 사용 가능합니다.</b><br><br>
        
        <b>요청 본문 예시:</b>
        <pre>
{
  "deviceId": "galaxy-watch-1234",
  "stressIndex": 75.5,
  "heartRate": 85,
 
}
        </pre>

        <b>필드 설명:</b>
        - deviceId: 등록된 기기 식별자 (최초 등록 시 발급된 ID) <b>[필수]</b><br>
        - stressIndex: 스트레스 지수 (0.0 ~ 100.0) <b>[필수]</b><br>
        - heartRate: 심박수 (정수값) <b>[필수]</b><br>
       

        <b>유효성 검사:</b>
        1. stressIndex: 0.0 ~ 100.0 사이 값
        2. heartRate: 30 ~ 200 사이 정수
       

        <b>응답:</b>
        - 200 OK: 데이터 저장 성공
        - 400 Bad Request: 유효성 검사 실패
        - 404 Not Found: 등록되지 않은 기기
        """
    )
    @PostMapping("/data")
    public ResponseEntity<?> submitData(@RequestBody @Valid SmartwatchRequest request) {
        try {
            smartwatchService.saveData(request); // 변경된 메서드 호출
            return ResponseEntity.ok("데이터 저장 성공");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    // 기기 데이터 조회
    @Operation(
            summary = "스마트워치 데이터 조회",
            description = """
        사용자의 스마트워치 데이터를 조회합니다.<br>
        <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
        
        <b>응답 예시:</b>
        <pre>
[
  {
    "deviceId": "galaxy-watch-1234",
    "stressIndex": 75.5,
    "heartRate": 85,
    "measuredAt": "2025-05-18T14:30:45"
  },
  {
    "deviceId": "galaxy-watch-1234",
    "stressIndex": 68.2,
    "heartRate": 78,
    "measuredAt": "2025-05-18T15:00:30"
  }
]
        </pre>

        <b>응답 필드:</b>
        - deviceId: 기기 식별자<br>
        - stressIndex: 스트레스 지수<br>
        - heartRate: 심박수<br>
        - measuredAt: 측정 시간 (KST)<br>

        <b>에러 코드:</b>
        - 401 Unauthorized: 인증 정보 없음
        """
    )
    @GetMapping("/smartwatch")
    public ResponseEntity<List<SmartwatchData>> getDeviceData(
            @AuthenticationPrincipal User user
    ) {
        List<SmartwatchData> data = smartwatchService.getDeviceData(user);
        return ResponseEntity.ok(data);
    }
}
