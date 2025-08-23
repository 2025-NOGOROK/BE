package com.example.Easeplan.api.Survey.controller;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;

import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.api.client.util.DateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Tag(name = "생활패턴 설문", description = "생활패턴설문 API")
@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class UserSurveyController {
    private final UserSurveyService surveyService;
    private final UserRepository userRepository;
    private final FlaskRecommendService flaskService;
    private final GoogleCalendarService calendarService;


    // 1. 설문 저장
    @Operation(
            summary = "생활패턴 및 스트레스 설문 제출",
            description = """
        사용자의 생활패턴, 스트레스 반응 및 완화 방법에 관한 설문 응답을 저장합니다.<br><br>
        
        <b>요청 본문 예시 (JSON):</b>
        <pre>
{
  "scheduleType": "루즈/타이트",
  "suddenChangePreferred": true,
  "chronotype": "저녁/아침",
  "preferAlone": "함께/혼자",
  "stressReaction": "감각 회피형/감각 추구형/감각 민감+회피형/감각 둔감형/감각 민감+추구형",
  "hasStressRelief": true,
  "stressReliefMethods": ["명상", "산책"]
}
        </pre>
        
        <b>필드 설명:</b><br>
        - <code>scheduleType</code>: 일정 관리 유형 ("루즈" 또는 "타이트")<br>
        - <code>suddenChangePreferred</code>: 갑작스러운 일정 변화를 선호하는지 여부 (true/false)<br>
        - <code>chronotype</code>: 생활 리듬 유형 ("아침" 또는 "저녁")<br>
        - <code>preferAlone</code>: 혼자 활동 선호 여부 ("혼자" 또는 "함께")<br>
        - <code>stressReaction</code>: 스트레스 반응 유형<br>
        &nbsp;&nbsp;&nbsp;&nbsp;("감각 회피형", "감각 추구형", "감각 민감+회피형", "감각 둔감형", "감각 민감+추구형")<br>
        - <code>hasStressRelief</code>: 스트레스 해소법 보유 여부 (true/false)<br>
        - <code>stressReliefMethods</code>: 스트레스 해소법 목록 (예: ["명상", "산책"])<br>
        <br>
        <b>설명:</b><br>
        - 본 설문은 맞춤형 일정 추천 및 스트레스 관리 서비스 제공을 위해 사용됩니다.<br>
        - <code>stressReliefMethods</code>는 <code>hasStressRelief</code>가 true일 때만 입력합니다.<br>
        - 모든 필드는 필수 입력이며, 선택지는 안내된 값 중 하나를 사용하세요.<br>
        <br>
        <b>예시 요청:</b><br>
        <code>POST /api/survey</code><br>
        <br>
        <b>응답:</b><br>
        - 200 OK: 설문 저장 성공<br>
        - 400 Bad Request: 필드 누락 또는 값 오류
        """
    )

    @PostMapping
    public ResponseEntity<?> submitSurvey(
            @RequestBody UserSurveyRequest request // 🔥 인증 없이 요청 받음
    ) {
        try {
            // 1. 이메일로 사용자 조회
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

            // 2. 설문 저장
            surveyService.saveSurvey(user, request);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}
