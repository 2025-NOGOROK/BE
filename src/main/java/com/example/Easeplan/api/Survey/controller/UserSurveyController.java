package com.example.Easeplan.api.Survey.controller;

import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survey")
public class UserSurveyController {
    private final UserSurveyService service;

    public UserSurveyController(UserSurveyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> submitSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserSurveyRequest request
    ) {
        // 1. Null 체크를 최상단에 추가
        if (userDetails == null) {
            // 401 Unauthorized 등 적절한 에러 반환
            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        }

        // 2. 정상 로직
        String email = userDetails.getUsername();
        service.saveSurvey(email, request);
        return ResponseEntity.ok().build();
    }

}
