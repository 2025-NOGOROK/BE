package com.example.Easeplan.api.Recommend.Short.controller;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.dto.TimeSlot;
import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.global.auth.domain.User;
import com.google.api.client.util.DateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.example.Easeplan.api.Survey.service.UserSurveyService;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Tag(name = "Short_Recommend", description = "짧은 쉼표")
@RestController
public class ShortController {

    private final UserRepository userRepository;
    private final UserSurveyService surveyService;
    private final GoogleCalendarService calendarService;
    private final FlaskRecommendService flaskService;

    @Autowired
    public ShortController(UserRepository userRepository,
                           UserSurveyService surveyService,
                           GoogleCalendarService calendarService,
                           FlaskRecommendService flaskService) {
        this.userRepository = userRepository;
        this.surveyService = surveyService;
        this.calendarService = calendarService;
        this.flaskService = flaskService;
    }

    @Operation(summary = "날짜별 짧은 추천 조회", description = """
        사용자의 설문 결과와 구글 캘린더 일정을 바탕으로,<br>
        <b>선택한 날짜의 전체 일정과 빈 시간대에 맞춘 맞춤형 짧은 쉼표(휴식/완화) 추천 일정을 함께 반환합니다.</b><br><br>
        - <b>인증(로그인) 필요</b><br>
        - 쿼리 파라미터 <code>date</code>에 조회할 날짜(yyyy-MM-dd)를 입력하세요.<br>
        - 응답에는 해당 날짜의 기존 일정과, AI/설문 기반 추천 일정이 함께 포함됩니다.<br>
        - 추천 일정은 설문 결과와 빈 시간대, Flask 추천 API 결과를 조합하여 생성됩니다.<br>
        <br>
        <b>예시 요청:</b><br>
        <code>GET /short-recommend?date=2025-05-18</code>
        """)
    @GetMapping("/api/short-recommend")
    public ResponseEntity<List<FormattedTimeSlot>> getShortRecommendation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String date // yyyy-MM-dd
    ) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).build();
            }
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

            UserSurvey survey = surveyService.getSurveyByUser(user);
            UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);

            String accessToken = user.getGoogleAccessToken();
            String refreshToken = user.getGoogleRefreshToken();
            ZonedDateTime startOfDay = LocalDate.parse(date).atStartOfDay(ZoneId.of("Asia/Seoul"));
            ZonedDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            List<FormattedTimeSlot> events = calendarService.getFormattedEvents(
                    user, "primary",
                    startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );

            List<TimeSlot> freeSlots = calendarService.getFreeTimeSlots(
                    user, LocalDate.parse(date)
            );
            // 06:00 이후 시작하는 슬롯만 필터링
            List<TimeSlot> filteredSlots = freeSlots.stream()
                    .filter(slot -> {
                        Instant startInstant = Instant.ofEpochMilli(slot.getStart().getValue());
                        ZonedDateTime startKST = startInstant.atZone(ZoneId.of("Asia/Seoul"));
                        return startKST.toLocalTime().isAfter(LocalTime.of(5, 59));
                    })
                    .collect(Collectors.toList());

            List<String> recommendations = flaskService.getRecommendations(request);

            List<FormattedTimeSlot> recommendEvents = new ArrayList<>();
            for (int i = 0; i < recommendations.size() && i < filteredSlots.size(); i++) {
                TimeSlot slot = filteredSlots.get(i);
                String title = recommendations.get(i);

                // (숫자분) 패턴에서 시간 추출
                int durationMinutes = 60; // 기본 1시간
                Pattern pattern = Pattern.compile("\\((\\d+)분\\)");
                Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    durationMinutes = Integer.parseInt(matcher.group(1));
                }

                // 시작 시간
                DateTime start = slot.getStart();
                // 종료 시간: 시작 + durationMinutes
                DateTime end = new DateTime(start.getValue() + durationMinutes * 60 * 1000);

                recommendEvents.add(new FormattedTimeSlot(
                        title,
                        "설문 기반 추천",
                        start.toStringRfc3339(),
                        end.toStringRfc3339()
                ));
            }

            List<FormattedTimeSlot> all = new ArrayList<>(events);
            all.addAll(recommendEvents);
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "추천 일정 생성 및 구글 캘린더 저장", description = """
        선택한 날짜의 빈 시간대에 대해 AI/설문 기반으로 추천 일정을 생성하고,<br>
        <b>해당 일정을 구글 캘린더에 자동 저장한 뒤 전체 일정을 반환합니다.</b><br><br>
        - <b>인증(로그인) 필요</b><br>
        - 쿼리 파라미터 <code>date</code>에 추천 및 저장할 날짜(yyyy-MM-dd)를 입력하세요.<br>
        - 추천 일정은 설문 결과와 Flask 추천 API 결과, 빈 시간대 정보를 바탕으로 자동 생성됩니다.<br>
        - 추천 일정은 구글 캘린더에 즉시 저장되며, 응답에는 기존 일정과 추천 일정이 모두 포함됩니다.<br>
        <br>
        <b>예시 요청:</b><br>
        <code>POST /short-recommend?date=2025-05-18</code>
        """)
    @PostMapping("/api/short-recommend")
    public ResponseEntity<List<FormattedTimeSlot>> createAndSaveShortRecommendation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String date // yyyy-MM-dd
    ) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401).build();
            }
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

            UserSurvey survey = surveyService.getSurveyByUser(user);
            UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);

            String accessToken = user.getGoogleAccessToken();
            String refreshToken = user.getGoogleRefreshToken();
            ZonedDateTime startOfDay = LocalDate.parse(date).atStartOfDay(ZoneId.of("Asia/Seoul"));
            ZonedDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            List<FormattedTimeSlot> events = calendarService.getFormattedEvents(
                    user, "primary",
                    startOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    endOfDay.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );

            List<TimeSlot> freeSlots = calendarService.getFreeTimeSlots(user, LocalDate.parse(date));

            // 06:00 이후 시작하는 슬롯만 필터링
            List<TimeSlot> filteredSlots = freeSlots.stream()
                    .filter(slot -> {
                        Instant startInstant = Instant.ofEpochMilli(slot.getStart().getValue());
                        ZonedDateTime startKST = startInstant.atZone(ZoneId.of("Asia/Seoul"));
                        return startKST.toLocalTime().isAfter(LocalTime.of(5, 59));
                    })
                    .collect(Collectors.toList());

            List<String> recommendations = flaskService.getRecommendations(request);

            List<FormattedTimeSlot> recommendEvents = new ArrayList<>();
            for (int i = 0; i < recommendations.size() && i < filteredSlots.size(); i++) {
                TimeSlot slot = filteredSlots.get(i);
                String title = recommendations.get(i);

                // (숫자분) 패턴에서 시간 추출
                int durationMinutes = 60; // 기본 1시간
                Pattern pattern = Pattern.compile("\\((\\d+)분\\)");
                Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    durationMinutes = Integer.parseInt(matcher.group(1));
                }

                DateTime start = slot.getStart();
                DateTime end = new DateTime(start.getValue() + durationMinutes * 60 * 1000);

                FormattedTimeSlot event = new FormattedTimeSlot(
                        title,
                        "설문 기반 추천",
                        start.toStringRfc3339(),
                        end.toStringRfc3339()
                );
                recommendEvents.add(event);

                // 구글 캘린더에 저장
                calendarService.addEvent(
                        user,
                        "primary",
                        event.getTitle(),
                        event.getDescription(),
                        start.toStringRfc3339(),
                        end.toStringRfc3339(),
                        false,
                        0,
                        false,
                        false
                );

            }

            List<FormattedTimeSlot> all = new ArrayList<>(events);
            all.addAll(recommendEvents);
            return ResponseEntity.ok(all);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
