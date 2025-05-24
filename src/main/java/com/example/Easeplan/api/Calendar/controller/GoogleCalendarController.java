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
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "GoogleCalendar", description = "구글캘린더 API")
@RestController
@RequestMapping("/auth/google")
public class GoogleCalendarController {
    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarService calendarService;
    private final NotificationScheduler notificationScheduler;
    private final UserRepository userRepository;

    public GoogleCalendarController(GoogleOAuthService oAuthService, GoogleCalendarService calendarService, NotificationScheduler notificationScheduler, UserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.calendarService = calendarService;
        this.notificationScheduler = notificationScheduler;
        this.userRepository = userRepository;
    }

    @Operation(summary = "구글 캘린더 인증 callback (최초 연동 및 리프레시 토큰 갱신 필요 시)")
    @GetMapping("/callback")
    public void oauth2Callback(@RequestParam String code, HttpServletResponse response) {
        try {
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
            Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(decodedCode);
            String accessToken = (String) tokenResponse.get("access_token");
            // refresh_token은 최초 발급 시에만 존재할 수 있습니다. 갱신 시에는 없을 수 있습니다.
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Long expiresIn = ((Number) tokenResponse.get("expires_in")).longValue();

            if (accessToken == null) {
                log.error("Access token is null after exchangeCodeForToken.");
                response.sendRedirect("/login/fail?reason=token");
                return;
            }
            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
            String email = (String) userInfo.get("email");
            if (email == null) {
                log.error("User email is null after getGoogleUserInfo.");
                response.sendRedirect("/login/fail?reason=email");
                return;
            }
            // 이메일로 사용자 조회 (User 엔티티의 email 필드는 unique)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));

            // **[핵심 변경]** User 엔티티에 토큰과 만료 시간 저장 및 DB 반영
            user.updateGoogleTokens(accessToken, refreshToken); // refreshToken은 null일 수도 있음 (갱신 시)
            // expiresIn은 초 단위이므로 현재 시각에 더해서 만료 시각을 계산하고 UTC 기준으로 저장
            user.setGoogleAccessTokenExpiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(expiresIn), ZoneOffset.UTC));
            userRepository.save(user); // DB에 토큰 정보 저장

            // 클라이언트(프론트엔드)로 성공 리다이렉트
            String appRedirectUrl = "https://recommend.ai.kr/callback.html?result=success";
            response.sendRedirect(appRedirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 Callback Error: {}", e.getMessage(), e);
            String appErrorUrl = "https://recommend.ai.kr/callback?result=fail&reason=exception";
            try {
                response.sendRedirect(appErrorUrl);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @Operation(
            summary = "구글 캘린더 일정 조회",
            description = """
        사용자의 구글 캘린더에서 지정한 기간의 일정을 조회합니다.<br>
        <br>
        **인증:**<br>
        - JWT(AccessToken) 필요<br>
        <br>
        **파라미터**<br>
        - calendarId (쿼리, 기본값 primary): 조회할 캘린더 ID<br>
        - !!!!시간에서 +는 반드시 %2B로 표현해야 한다!!!!
        - timeMin (쿼리, 필수): 조회 시작 시각 (RFC3339, 예: 2025-05-10T10:00:00%2B09:00)<br>
        - timeMax (쿼리, 필수): 조회 종료 시각 (RFC3339, 예: 2025-05-31T23:59:59%2B09:00)<br>
        <br>
        **응답 예시**<br>
        ```
        [
          {
            "title": "회의",
            "startDateTime": "2025-05-10T10:00:00+09:00",
            "endDateTime": "2025-05-10T11:00:00+09:00",
            "description": "주간 회의"
          }
        ]
        ```
        <br>
        **참고**<br>
        - timeMin, timeMax의 +는 반드시 %2B로 인코딩해야 합니다.
        """
    )

    @GetMapping("/events")
    public ResponseEntity<List<FormattedTimeSlot>> getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String timeMin,
            @RequestParam String timeMax
    ) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<FormattedTimeSlot> events = calendarService.getFormattedEvents(user, calendarId, timeMin, timeMax);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("구글 캘린더 이벤트 추가 중 예외 발생", e);
            // 필요하다면 사용자에게 적절한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




    @Operation(
            summary = "구글 캘린더 일정 추가",
            description = """
        사용자의 구글 캘린더에 새로운 일정을 추가합니다.<br>
        <br>
        **인증:**<br>
        - JWT(AccessToken) 필요<br>
        <br>
        **요청 바디 예시**<br>
        ```
        {
          "calendarId": "primary",
          "title": "스터디",
          "description": "알고리즘 스터디",
          "startDateTime": "2025-06-01T14:00:00+09:00",
          "endDateTime": "2025-06-01T16:00:00+09:00",
          "serverAlarm": true,
          "minutesBeforeAlarm": 30,
          "fixed": false,
          "userLabel": true
        }
        ```
        <br>
        **응답**<br>
        - 생성된 구글 이벤트 객체 반환
        """
    )

    @PostMapping("/eventsPlus")
    public ResponseEntity<Event> addEvent(
            @RequestBody CalendarEventRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event created = calendarService.addEvent(
                user, // User 객체 전달
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

        // FCM 알림 스케줄링 로직 (기존 코드 유지)
        if (req.serverAlarm && req.minutesBeforeAlarm > 0) {
            ZonedDateTime eventStart = ZonedDateTime.parse(req.startDateTime)
                    .withZoneSameInstant(ZoneId.of("Asia/Seoul"));
            Instant alarmTime = eventStart
                    .minusMinutes(req.minutesBeforeAlarm)
                    .toInstant();
            user.getFcmTokens().forEach(token -> {
                try {
                    notificationScheduler.scheduleAlarm(
                            token,
                            "🔔 " + req.title,
                            req.minutesBeforeAlarm + "분 후 일정 시작!",
                            alarmTime
                    );
                } catch (Exception e) {
                    user.removeFcmToken(token); // 실패한 토큰 제거
                    log.error("FCM 전송 실패: {}", e.getMessage());
                }
            });
            userRepository.save(user); // 토큰 상태 저장
        }

        return ResponseEntity.ok(created);
    }

    @Operation(summary = "구글캘린더 일정 삭제")
    @DeleteMapping("/eventsPlus")
    public ResponseEntity<Void> deleteEvent(
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String eventId,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        calendarService.deleteEvent(user, calendarId, eventId); // User 객체 전달
        return ResponseEntity.noContent().build();
    }
}