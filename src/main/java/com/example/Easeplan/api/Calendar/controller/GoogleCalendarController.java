package com.example.Easeplan.api.Calendar.controller;

import com.example.Easeplan.api.Calendar.dto.CalendarEventRequest;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import com.example.Easeplan.api.Fcm.service.NotificationScheduler;
import com.google.api.services.calendar.model.Event;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.google.api.services.calendar.model.Events;

@RestController
@RequestMapping("/auth/google")
public class GoogleCalendarController {
    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarService calendarService;
    private final NotificationScheduler notificationScheduler;

    public GoogleCalendarController(GoogleOAuthService oAuthService, GoogleCalendarService calendarService,NotificationScheduler notificationScheduler) {
        this.oAuthService = oAuthService;
        this.calendarService = calendarService;
        this.notificationScheduler = notificationScheduler;
    }

    // 구글 인증 콜백 (authorization code 수신)
    @GetMapping("/callback")
    public Map<String, Object> oauth2Callback(@RequestParam String code) {
        // code로 access token 교환
        Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(code);
        // access_token, refresh_token 등 반환
        return tokenResponse;
    }

    // 일정 조회 (헤더에서 토큰 추출)
    @GetMapping("/events")
    public Events getEvents(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String timeMin,  // "2025-05-31T00:00:00+09:00" 형식
            @RequestParam String timeMax   // "2025-05-31T23:59:59+09:00" 형식
    ) throws Exception {
        String accessToken = extractBearerToken(authorization);
        return calendarService.getEvents(accessToken, calendarId, timeMin, timeMax);
    }

    // access token으로 일정 조회 (날짜 범위 추가)
    @GetMapping("/free-time")
    public ResponseEntity<List<FormattedTimeSlot>> getFormattedFreeTimeSlots(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) throws Exception {
        String accessToken = extractBearerToken(authorization);
        List<FormattedTimeSlot> slots = calendarService.getFormattedFreeTimeSlots(accessToken, date);
        return ResponseEntity.ok(slots);
    }

    // 일정 추가 (예시: accessToken이 필요하면 마찬가지로 헤더에서 추출)
    @PostMapping("/eventsPlus")
    public ResponseEntity<Event> addEvent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody CalendarEventRequest req) throws Exception {
        String accessToken = extractBearerToken(authorization);
        Event created = calendarService.addEvent(
                accessToken,
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
    @DeleteMapping("/eventsPlus")
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String eventId
    ) throws Exception {
        String accessToken = extractBearerToken(authorization);
        calendarService.deleteEvent(accessToken, calendarId, eventId);
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
