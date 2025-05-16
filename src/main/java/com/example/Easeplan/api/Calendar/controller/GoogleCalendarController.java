package com.example.Easeplan.api.Calendar.controller;

import com.example.Easeplan.api.Calendar.dto.CalendarEventRequest;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import com.example.Easeplan.api.Fcm.service.NotificationScheduler;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.api.services.calendar.model.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.google.api.services.calendar.model.Events;
@Tag(name = "GoogleCalendar", description = "구글캘린더 API")
@RestController
@RequestMapping("/auth/google")
public class GoogleCalendarController {
    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarService calendarService;
    private final NotificationScheduler notificationScheduler;
    private final UserRepository userRepository; // ✅ 생성자에 추가


    public GoogleCalendarController(GoogleOAuthService oAuthService, GoogleCalendarService calendarService,NotificationScheduler notificationScheduler,UserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.calendarService = calendarService;
        this.notificationScheduler = notificationScheduler;
        this.userRepository = userRepository;
    }

    // 구글 인증 콜백 (authorization code 수신)
    @Operation(summary = "구글 캘린더 인증 callback", description = """ 
            인증을 통해 구글캘린더를 연동합니다.""")
    @GetMapping("/callback")
    public ResponseEntity<?> oauth2Callback(@RequestParam String code) {
        try {
            Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(code);
            String accessToken = (String) tokenResponse.get("access_token");
            if (accessToken == null) {
                return ResponseEntity.status(400).body("토큰 발급 실패: access_token이 없습니다.");
            }
            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
            String email = (String) userInfo.get("email");
            if (email == null) {
                return ResponseEntity.status(400).body("구글 userinfo에서 email을 가져올 수 없습니다.");
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            user.updateGoogleTokens(
                    accessToken,
                    (String) tokenResponse.get("refresh_token")
            );
            userRepository.save(user);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            // 에러 로그 남기기
            e.printStackTrace();
            return ResponseEntity.status(500).body("구글 인증 중 오류 발생: " + e.getMessage());
        }
    }

    // 일정 조회 (헤더에서 토큰 추출)
    @Operation(summary = "구글캘린더 조회", description = """
            구글 캘린더를 조회합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @GetMapping("/events")
    public Events getEvents(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String timeMin,  // "2025-05-31T00:00:00+09:00" 형식
            @RequestParam String timeMax,   // "2025-05-31T23:59:59+09:00" 형식
            @AuthenticationPrincipal UserDetails userDetails // ✅ 추가
    ) throws Exception {
        String accessToken = extractBearerToken(authorization);
        // ✅ 사용자 refreshToken 조회
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String refreshToken = user.getGoogleRefreshToken();


        return calendarService.getEvents(accessToken, refreshToken,calendarId, timeMin, timeMax);
    }

    // access token으로 일정 조회 (날짜 범위 추가)
//    @Operation(summary = "구글캘린더 특정 일정으로 빈 일정 조회", description = """
//            특정 날에 구글 캘린더를 조회합니다.<br>
//            헤더에 accessToken을 넣어주세요.<br>
//            """)
//    @GetMapping("/free-time")
//    public ResponseEntity<List<FormattedTimeSlot>> getFormattedFreeTimeSlots(
//            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
//            @AuthenticationPrincipal UserDetails userDetails // ✅ 추가
//    ) throws Exception {
//        String accessToken = extractBearerToken(authorization);
//        // ✅ 사용자 refreshToken 조회
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        String refreshToken = user.getGoogleRefreshToken();
//        List<FormattedTimeSlot> slots = calendarService.getFormattedFreeTimeSlots(accessToken,refreshToken, date);
//        return ResponseEntity.ok(slots);
//    }

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    // 일정 추가 (예시: accessToken이 필요하면 마찬가지로 헤더에서 추출)
    @Operation(summary = "구글캘린더에 일정을 추가", description = """
            특정 날에 구글 캘린더에 일정을 추가합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @PostMapping("/eventsPlus")
    public ResponseEntity<Event> addEvent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody CalendarEventRequest req, @AuthenticationPrincipal UserDetails userDetails // ✅ 추가
            ) throws Exception {
        String accessToken = extractBearerToken(authorization);
// ✅ 현재 사용자 조회
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ 인스턴스 메서드 호출
        String refreshToken = user.getGoogleRefreshToken(); // ✅ refreshToken 조회

        Event created = calendarService.addEvent(

                user.getGoogleAccessToken(),
                refreshToken,
                req.calendarId,
                req.title,
                req.description,
                req.startDateTime,
                req.endDateTime,
                req.serverAlarm,
                req.minutesBeforeAlarm,
                req.aiRecommend,
                req.fixed,
                req.userLabel
        );
        // 1. 서버 알림을 사용한다면
        if (req.serverAlarm) {
            // 2. 알림 보낼 시각 계산 (일정 시작 - minutesBeforeAlarm)
            java.time.ZonedDateTime eventStart = java.time.ZonedDateTime.parse(req.startDateTime);
            java.time.Instant alarmTime = eventStart.minusMinutes(req.minutesBeforeAlarm).toInstant();

            // 3. FCM 토큰, 알림 제목/내용은 필요에 맞게 세팅
            String userFcmToken = "사용자_FCM_토큰"; // 실제로는 DB 등에서 조회
            String title = req.title;
            String body = req.description;

            notificationScheduler.scheduleAlarm(userFcmToken, title, body, alarmTime);
        }

        return ResponseEntity.ok(created);
    }

    //일정 삭제
    @Operation(summary = "구글캘린더에 일정을 삭제", description = """
            특정 날에 구글 캘린더에 일정을 삭제합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @DeleteMapping("/eventsPlus")
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String eventId,
            @AuthenticationPrincipal UserDetails userDetails // ✅ 추가
    ) throws Exception {
        String accessToken = extractBearerToken(authorization);
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ 인스턴스 메서드 호출
        String refreshToken = user.getGoogleRefreshToken(); // ✅ refreshToken 조회

        calendarService.deleteEvent(accessToken, refreshToken,calendarId, eventId);
        return ResponseEntity.noContent().build();
    }

    // Bearer 토큰 추출 유틸
    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new IllegalArgumentException("Authorization header must be provided in 'Bearer {token}' format.");
    }

}
