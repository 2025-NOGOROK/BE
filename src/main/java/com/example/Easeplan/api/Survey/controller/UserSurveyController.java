package com.example.Easeplan.api.Survey.controller;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
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

import java.time.LocalDate;
import java.util.List;

@Tag(name = "생활패턴 설문", description = "생활패턴설문 API")
@RestController
@RequestMapping("/api/survey")
public class UserSurveyController {
    private final UserSurveyService service;
    private final UserRepository userRepository;
    private final FlaskRecommendService flaskRecommendService;
    private final GoogleCalendarService calendarService;

    public UserSurveyController(
            UserSurveyService service,
            UserRepository userRepository,
            FlaskRecommendService flaskRecommendService,
            GoogleCalendarService calendarService
    ) {
        this.service = service;
        this.userRepository = userRepository;
        this.flaskRecommendService = flaskRecommendService;
        this.calendarService = calendarService;
    }

    @Operation(summary = "생활패턴", description = "생활패턴응답을 저장하고 짧은 추천을 합니다.")
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
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

        // 1. 설문 저장
        service.saveSurvey(user, request);

        // 2. Flask 추천 API 호출
        List<String> recommendations = flaskRecommendService.getRecommendations(request);

        // 3. 구글 캘린더 빈 시간대 조회 및 추천 일정 추가
        try {
            LocalDate today = LocalDate.now();
            List<FormattedTimeSlot> freeSlots = calendarService.getFormattedFreeTimeSlots(
                    user.getGoogleAccessToken(),
                    user.getGoogleRefreshToken(), // 반드시 추가!
                    today
            );

            for (int i = 0; i < Math.min(recommendations.size(), freeSlots.size()); i++) {
                FormattedTimeSlot slot = freeSlots.get(i);
                calendarService.addEvent(
                        user.getGoogleAccessToken(),
                        user.getGoogleRefreshToken(), // 반드시 추가!
                        "primary",
                        recommendations.get(i),
                        "추천 활동",
                        slot.getStartDateTime(), // 반드시 ISO8601 문자열 (예: 2025-05-14T09:00:00+09:00)
                        slot.getEndDateTime(),
                        false,
                        0,
                        false,
                        false,
                        true
                );
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("캘린더 추가 실패: " + e.getMessage());
        }

        return ResponseEntity.ok(recommendations);
    }
}
