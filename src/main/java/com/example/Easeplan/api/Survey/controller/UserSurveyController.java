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


    // 2. 시나리오 생성 (구글 일정, 설문 추천 일정, 스트레스 추천 일정)
//    @Operation(summary = " 긴 추천 시나리오 생성", description = "3가지 일정 시나리오를 생성합니다.(아직 개발중)")
//    @PostMapping("/scenarios")
//    public ResponseEntity<ScenarioResponse> generateScenarios(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestHeader("accessToken") String accessToken,
//            @RequestHeader("refreshToken") String refreshToken
//    ) {
//        if (userDetails == null) {
//            return ResponseEntity.status(401).body(null);
//        }
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));
//        UserSurvey survey = surveyService.getSurveyByUser(user);
//        UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);
//
//        LocalDate today = LocalDate.now();
//        LocalDate twoWeeksLater = today.plusWeeks(2);
//        ZonedDateTime timeMin = today.atStartOfDay(ZoneId.of("Asia/Seoul"));
//        ZonedDateTime timeMax = twoWeeksLater.atTime(23, 59).atZone(ZoneId.of("Asia/Seoul"));
//        String timeMinStr = timeMin.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//        String timeMaxStr = timeMax.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//
//        // 1. 구글 일정 기반 시나리오
//        List<FormattedTimeSlot> googleSlots;
//        try {
//            googleSlots = calendarService.getFormattedEvents(
//                    accessToken,
//                    refreshToken,
//                    "primary",
//                    timeMinStr,
//                    timeMaxStr
//            );
//        } catch (Exception e) {
//            log.error("구글 캘린더 일정 조회 실패", e);
//            return ResponseEntity.internalServerError().body(null);
//        }
//        ScheduleScenario googleScenario = new ScheduleScenario("GOOGLE", googleSlots, List.of());
//
//        // 2. 설문 기반 시나리오 (구글 일정 + 설문 추천 일정)
//        List<TimeSlot> freeSlots;
//        try {
//            freeSlots = calendarService.getFreeTimeSlots(accessToken, refreshToken, LocalDate.now());
//        } catch (Exception e) {
//            log.error("빈 시간대 조회 실패", e);
//            return ResponseEntity.internalServerError().body(null);
//        }
//
//        List<String> surveyRecommendations = flaskService.getRecommendations(request);
//
//        List<FormattedTimeSlot> surveyEvents = new ArrayList<>();
//        for (int i = 0; i < surveyRecommendations.size() && i < freeSlots.size(); i++) {
//            TimeSlot slot = freeSlots.get(i);
//            String startDateTime = new DateTime(slot.getStart().getValue()).toStringRfc3339();
//            String endDateTime = new DateTime(slot.getEnd().getValue()).toStringRfc3339();
//
//            surveyEvents.add(new FormattedTimeSlot(
//                    surveyRecommendations.get(i),
//                    "설문 기반 추천 활동",
//                    startDateTime,
//                    endDateTime
//            ));
//        }
//
//        List<FormattedTimeSlot> combinedSurveyEvents = new ArrayList<>();
//        combinedSurveyEvents.addAll(googleSlots); // 기존 일정
//        combinedSurveyEvents.addAll(surveyEvents); // 빈 시간에 배치된 추천 일정
//
//        ScheduleScenario surveyScenario = new ScheduleScenario("SURVEY", combinedSurveyEvents, surveyRecommendations);
//
//        // 3. 스트레스+설문 기반 시나리오 (임시로 빈 값)
//        List<String> stressRecommendations = List.of();
//        List<FormattedTimeSlot> stressEvents = List.of();
//        ScheduleScenario stressScenario = new ScheduleScenario("STRESS", stressEvents, stressRecommendations);
//
//        // 임시 저장 및 반환
//        String selectionKey = scenarioStorage.store(new ScenarioPack(googleScenario, surveyScenario, stressScenario));
//
//        return ResponseEntity.ok(new ScenarioResponse(
//                List.of(googleScenario, surveyScenario, stressScenario),
//                selectionKey
//        ));
//    }
//
//    // 3. 시나리오 조회 (GET)
//    @Operation(summary = "유형별 시나리오 조회", description = "GOOGLE, SURVEY, STRESS 3가지 중 원하는 유형의 시나리오를 조회합니다.")
//    @GetMapping("/scenarios")
//    public ResponseEntity<ScenarioResponse> getScenarios(
//            @RequestParam String selectionKey,
//            @RequestParam(required = false) String type
//    ) {
//        ScenarioPack pack = scenarioStorage.get(selectionKey);
//        if (pack == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // 🔥 ScenarioResponse로 감싸서 반환
//        return switch (type != null ? type.toUpperCase() : "") {
//            case "GOOGLE" -> ResponseEntity.ok(
//                    new ScenarioResponse(List.of(pack.google()), selectionKey)
//            );
//            case "SURVEY" -> ResponseEntity.ok(
//                    new ScenarioResponse(List.of(pack.survey()), selectionKey)
//            );
//            case "STRESS" -> ResponseEntity.ok(
//                    new ScenarioResponse(List.of(pack.stress()), selectionKey)
//            );
//            default -> ResponseEntity.ok(
//                    new ScenarioResponse(List.of(pack.google(), pack.survey(), pack.stress()), selectionKey)
//            );
//        };
//    }

//    // 4. 시나리오 선택 및 확정
//    @Operation(summary = "시나리오 선택", description = "선택된 시나리오를 실제 캘린더에 반영합니다.")
//    @PostMapping("/select")
//    public ResponseEntity<?> selectScenario(
//            @RequestParam String selectionKey,
//            @RequestParam String scenarioType,
//            @AuthenticationPrincipal UserDetails userDetails
//    ) {
//        if (userDetails == null) {
//            return ResponseEntity.status(401).body("인증 정보가 없습니다.");
//        }
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));
//
//        ScenarioPack pack = scenarioStorage.retrieve(selectionKey);
//        if (pack == null) {
//            return ResponseEntity.badRequest().body("시나리오가 만료되었거나 존재하지 않습니다.");
//        }
//
//        ScheduleScenario selected = switch (scenarioType.toUpperCase()) {
//            case "GOOGLE" -> pack.google();
//            case "SURVEY" -> pack.survey();
//            case "STRESS" -> pack.stress();
//            default -> null;
//        };
//
//        if (selected == null) {
//            return ResponseEntity.badRequest().body("시나리오 타입이 잘못되었습니다.");
//        }
//
//        try {
//            for (FormattedTimeSlot slot : selected.events()) {
//                calendarService.addEvent(
//                        user.getGoogleAccessToken(),
//                        user.getGoogleRefreshToken(),
//                        "primary",
//                        slot.getTitle() != null ? slot.getTitle() : "생활패턴 일정",
//                        slot.getDescription() != null ? slot.getDescription() : "설문 기반 생성",
//                        slot.getStartDateTime(),
//                        slot.getEndDateTime(),
//                        false,
//                        0,
//                        false,
//                        false,
//                        true
//                );
//            }
//        } catch (Exception e) {
//            log.error("캘린더 이벤트 추가 실패", e);
//            return ResponseEntity.internalServerError().body("캘린더 이벤트 추가 실패: " + e.getMessage());
//        }
//
//        return ResponseEntity.ok().build();
//    }
}
