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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import com.google.api.services.calendar.model.Events;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
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
    @Operation(
            summary = "구글 캘린더 인증 callback",
            description = """
        구글 OAuth2 인증 후(URL) authorization code를 받아 accessToken/refreshToken을 발급받고,
        해당 사용자의 계정에 구글 캘린더 연동 정보를 저장합니다.<br><br>
        <b>요청 예시:</b><br>
        <code>GET /auth/google/callback?code=인증코드</code><br><br>
        <b>동작:</b><br>
        1. 인증 코드(code)를 받아 구글 OAuth 서버에서 accessToken, refreshToken을 발급받습니다.<br>
        2. accessToken으로 구글 사용자 정보를 조회하여 이메일을 확인합니다.<br>
        3. 해당 이메일로 가입된 사용자의 DB에 토큰 정보를 저장합니다.<br>
        4. 토큰 정보(accessToken, refreshToken, 만료 시간 등)를 응답으로 반환합니다.<br>
        <br>
        <b>응답 예시:</b><br>
        <pre>
{
  "access_token": "ya29.a0AbV...",
  "refresh_token": "1//0ejh...",
  "expires_in": 3599,
  ...
}
        </pre>
        <b>에러:</b><br>
        - 400: 토큰 발급 실패, 이메일 조회 실패 등<br>
        - 500: 서버 내부 오류
        """
    )

    @GetMapping("/callback")
    public void oauth2Callback(@RequestParam String code, HttpServletResponse response) {
        try {
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
            Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(decodedCode);
            String accessToken = (String) tokenResponse.get("access_token");
            if (accessToken == null) {
                response.sendRedirect("/login/fail?reason=token");
                return;
            }
            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
            String email = (String) userInfo.get("email");
            if (email == null) {
                response.sendRedirect("/login/fail?reason=email");
                return;
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            user.updateGoogleTokens(
                    accessToken,
                    (String) tokenResponse.get("refresh_token")
            );
            userRepository.save(user);

            String appRedirectUrl = "https://recommend.ai.kr/callback.html?result=success";
            response.sendRedirect(appRedirectUrl);
        } catch (Exception e) {
            String appErrorUrl = "https://recommend.ai.kr/callback?result=fail&reason=exception";
            try {
                response.sendRedirect(appErrorUrl);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }


    // 일정 조회 (헤더에서 토큰 추출)
    @Operation(
            summary = "구글캘린더 일정 조회",
            description = """
        사용자의 구글 캘린더에서 지정한 기간의 일정을 조회합니다.<br>
        <b>헤더에 Authorization: Bearer {accessToken}을(구글 OAuth에서 받은) 반드시 포함해야 합니다.</b><br><br>
        <b>요청 파라미터:</b><br>
        - calendarId: 캘린더 ID (기본값: primary)<br>
        - timeMin: 조회 시작 시간 (ISO 8601, 예: 2025-05-31T00:00:00+09:00)<br>
        - timeMax: 조회 종료 시간 (ISO 8601, 예: 2025-05-31T23:59:59+09:00)<br>
        <br>
        <b>동작:</b><br>
        1. accessToken과 refreshToken으로 구글 캘린더 API에서 일정을 조회합니다.<br>
        2. 해당 기간 내의 모든 일정을 반환합니다.<br>
        <br>
        <b>응답 예시:</b><br>
        <pre>
{
  "items": [
    {
      "id": "abcd1234",
      "summary": "회의",
      "start": {"dateTime": "2025-05-31T10:00:00+09:00"},
      "end": {"dateTime": "2025-05-31T11:00:00+09:00"}
    },
    ...
  ]
}
        </pre>
        <b>에러:</b><br>
        - 401: 인증 실패(토큰 만료/누락)<br>
        - 400: 파라미터 오류<br>
        """
    )
    @GetMapping("/events")
    public ResponseEntity<String> getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String timeMin,
            @RequestParam String timeMax
    ) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String accessToken = oAuthService.getOrRefreshGoogleAccessToken(user);

        String url = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events")
                .queryParam("timeMin", timeMin)
                .queryParam("timeMax", timeMax)
                .queryParam("singleEvents", "true")
                .queryParam("orderBy", "startTime")
                .build().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("구글 API 에러: {} / {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("알 수 없는 에러: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
        }
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
    @Operation(
            summary = "구글캘린더 일정 추가",
            description = """
        사용자의 구글 캘린더에 새 일정을 추가합니다.<br>
        <b>헤더에 Authorization: Bearer {accessToken}을(구글 OAuth에서 받은) 반드시 포함해야 합니다.</b><br><br>
        <b>요청 본문 예시 (CalendarEventRequest):</b>
        <pre>
{
  "calendarId": "primary",
  "title": "회의",
  "description": "팀 미팅",
  "startDateTime": "2025-05-18T14:00:00+09:00",
  "endDateTime": "2025-05-18T15:00:00+09:00",
  "serverAlarm": true,
  "minutesBeforeAlarm": 10,
  "aiRecommend": false,
  "fixed": false,
  "userLabel": true
}
        </pre>
        <b>동작:</b><br>
        1. 구글 캘린더에 일정을 추가합니다.<br>
        2. serverAlarm=true이고 minutesBeforeAlarm>0이면, 일정 시작 N분 전에 FCM 푸시 알림을 예약합니다.<br>
        3. 알림 예약 실패 시 해당 FCM 토큰은 DB에서 제거됩니다.<br>
        <br>
        <b>응답 예시:</b><br>
        <pre>
{
  "id": "abcd1234",
  "summary": "회의",
  "start": {"dateTime": "2025-05-18T14:00:00+09:00"},
  "end": {"dateTime": "2025-05-18T15:00:00+09:00"}
}
        </pre>
        <b>에러:</b><br>
        - 401: 인증 실패(토큰 만료/누락)<br>
        - 400: 파라미터 오류<br>
        - 500: 서버 내부 오류
        """
    )

    @PostMapping("/eventsPlus")
    public ResponseEntity<Event> addEvent(
            @RequestBody CalendarEventRequest req,
            @AuthenticationPrincipal UserDetails userDetails
            ) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String refreshToken = user.getGoogleRefreshToken();

        // 서버에서 accessToken을 직접 갱신/관리
        String accessToken = oAuthService.getOrRefreshGoogleAccessToken(user);
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
                req.fixed,
                req.userLabel
        );
        // 1. 서버 알림을 사용한다면
        // 3. 알림 스케줄링 (서버 알림 활성화 시)
        if (req.serverAlarm && req.minutesBeforeAlarm > 0) {
            // 시간대 변환 (UTC → KST)
            ZonedDateTime eventStart = ZonedDateTime.parse(req.startDateTime)
                    .withZoneSameInstant(ZoneId.of("Asia/Seoul"));

            // 알림 시간 계산
            Instant alarmTime = eventStart
                    .minusMinutes(req.minutesBeforeAlarm)
                    .toInstant();

            // 4. 모든 기기에 알림 전송
            user.getFcmTokens().forEach(token -> {
                try {
                    notificationScheduler.scheduleAlarm(
                            token,
                            "🔔 " + req.title,
                            req.minutesBeforeAlarm + "분 후 일정 시작!",
                            alarmTime
                    );
                } catch (Exception e) {
                    // 실패한 토큰 제거
                    user.removeFcmToken(token);
                    log.error("FCM 전송 실패: {}", e.getMessage());
                }
            });

            userRepository.save(user); // 토큰 상태 저장
        }

        return ResponseEntity.ok(created);
    }

    //일정 삭제
    @Operation(
            summary = "구글캘린더 일정 삭제",
            description = """
        사용자의 구글 캘린더에서 일정을 삭제합니다.<br>
        <b>헤더에 Authorization: Bearer {accessToken}을(구글 OAuth에서 받은) 반드시 포함해야 합니다.</b><br><br>
        <b>요청 파라미터:</b><br>
        - calendarId: 캘린더 ID (기본값: primary)<br>
        - eventId: 삭제할 일정의 고유 ID<br>
        <br>
        <b>동작:</b><br>
        1. 구글 캘린더에서 해당 일정을 삭제합니다.<br>
        <br>
        <b>응답:</b><br>
        - 204 No Content: 삭제 성공<br>
        <b>에러:</b><br>
        - 401: 인증 실패(토큰 만료/누락)<br>
        - 400: 파라미터 오류<br>
        - 404: 해당 일정 없음
        """
    )

    @DeleteMapping("/eventsPlus")
    public ResponseEntity<Void> deleteEvent(
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String eventId,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String refreshToken = user.getGoogleRefreshToken();
        String accessToken = oAuthService.getOrRefreshGoogleAccessToken(user);

        calendarService.deleteEvent(accessToken, refreshToken, calendarId, eventId);
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
