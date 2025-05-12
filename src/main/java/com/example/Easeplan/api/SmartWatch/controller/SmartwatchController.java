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
    @Operation(summary = "스마트 워치 등록", description = """
            스마트 워치를 등록합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @PostMapping("/smartwatch")
    public ResponseEntity<?> connectSmartwatch(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid SmartwatchRequest request
    ) {
        try {
            smartwatchService.connectDevice(user, request);
            return ResponseEntity.ok().body("기기 연결 성공");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 기기 데이터 조회
    @Operation(summary = "스마트 워치 조회", description = """
            스마트 워치를 조회합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @GetMapping("/smartwatch")
    public ResponseEntity<List<SmartwatchData>> getDeviceData(
            @AuthenticationPrincipal User user
    ) {
        List<SmartwatchData> data = smartwatchService.getDeviceData(user);
        return ResponseEntity.ok(data);
    }
}
