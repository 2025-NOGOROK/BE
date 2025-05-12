package com.example.Easeplan.api.Survey.controller;

import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "생활패턴 설문", description = "생활패턴설문 API")
@RestController
@RequestMapping("/api/survey")
public class UserSurveyController {
    private final UserSurveyService service;
    private final UserRepository userRepository; // 추가

    public UserSurveyController(
            UserSurveyService service,
            UserRepository userRepository // 주입 받음
    ) {
        this.service = service;
        this.userRepository = userRepository;
    }
    @Operation(summary = "생활패턴", description = """ 
            생활패턴응답을 저장합니다.""")
    @PostMapping
    public ResponseEntity<?> submitSurvey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserSurveyRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        }

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음")); // 추가

        service.saveSurvey(user, request); // ✅ user 객체 전달
        return ResponseEntity.ok().build();
    }
}
