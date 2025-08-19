package com.example.Easeplan.api.Calendar.controller;

import com.example.Easeplan.api.Calendar.dto.CalendarEventRequest;
import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import com.example.Easeplan.api.Fcm.service.NotificationScheduler;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.dto.JwtUtil;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.model.Event;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Tag(name = "GoogleCalendar", description = "구글캘린더 API")
@RestController
@RequestMapping("/auth/google")
public class GoogleCalendarController {

//    @Value("${google.client-id}")
//    private String googleClientId;
    private final GoogleOAuthService oAuthService;
    private final GoogleCalendarService calendarService;
    private final NotificationScheduler notificationScheduler;
    private final UserRepository userRepository;
    @Autowired
    private GoogleCalendarService googleCalendarService;


    @Autowired
    private JwtUtil jwtUtil;
    private final JwtUtil jwtProvider;

    public GoogleCalendarController(
            GoogleOAuthService oAuthService,
            GoogleCalendarService calendarService,
            NotificationScheduler notificationScheduler,
            UserRepository userRepository,
            JwtUtil jwtProvider) {
        this.oAuthService = oAuthService;
        this.calendarService = calendarService;
        this.notificationScheduler = notificationScheduler;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }





    /**
     * Google OAuth 인증 콜백 처리 (authorization code 받기)
     * @param code Google 인증 후 받은 authorization code
     * @return 액세스 토큰과 리프레시 토큰을 프론트엔드로 반환
     */
    @Operation(
            summary = "Google OAuth Callback 처리",
            description = """
        이 엔드포인트는 Google OAuth 인증 후, 전달된 authorization code를 이용하여
        access_token과 refresh_token을 요청하고, 이를 바탕으로 사용자의 구글 정보를 조회하여 
        사용자 정보를 DB에 저장하거나 갱신합니다.<br><br>

        **처리 흐름:**<br>
        1. 받은 authorization code로 액세스 토큰과 리프레시 토큰을 요청합니다.<br>
        2. 구글 사용자 정보를 조회하여 이메일을 확인하고, 기존 사용자가 있다면 업데이트합니다.<br>
        3. 새로운 구글 액세스 토큰, 리프레시 토큰, 만료 시각을 DB에 저장합니다.<br>
        4. 최종적으로 JWT를 발급하여 클라이언트에게 반환합니다.<br><br>

        **요청 예시:**<br>
        `GET /auth/google/callback?code=authorization_code_from_google`<br><br>

        **응답 예시:**<br>
        ```json
        {
          "access_token": "ya29.a0AVvZV...",
          "refresh_token": "1//0g7ZxV...",
          "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        }
        ```
        **응답 설명:**<br>
        - `access_token`: 구글 액세스 토큰<br>
        - `refresh_token`: 구글 리프레시 토큰<br>
        - `jwt`: 백엔드 시스템의 인증을 위한 JWT
    """
    )
    @GetMapping("/callback")
    public RedirectView googleCallback(@RequestParam("code") String code) {
        try {
            // 1. 받은 code로 액세스 토큰과 리프레시 토큰을 받음
            Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(code);
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            // 2. 구글 사용자 정보 조회
            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
            String email = (String) userInfo.get("email");

            // 3. 사용자 정보 업데이트 또는 새로 생성
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. Google 액세스 토큰 및 리프레시 토큰을 User에 저장
            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);
            user.setGoogleAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(7200)); // 예시: 1시간 후 만료
            user.setJwtToken(jwtProvider.createToken(user.getEmail())); // JWT 생성 후 저장

            // 5. DB에 저장 (토큰 갱신)
            userRepository.save(user);

            // 6. JWT 발급
            String jwtToken = jwtProvider.createToken(user.getEmail());

            // 7. 딥링크 URL 생성 (JWT를 앱으로 리디렉션)
            String redirectUri = "intent://oauth2callback?jwt=" + jwtToken + "#Intent;scheme=com.example.nogorok;package=com.example.nogorok;end";

            // 8. 앱 딥링크 URI로 리디렉션
            return new RedirectView(redirectUri); // 리디렉션 URL을 반환

        } catch (Exception e) {
            log.error("구글 인증 처리 중 오류", e);
            return new RedirectView("/error"); // 오류 발생 시 에러 페이지로 리디렉션
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
            "sourceType" : "calendar"
          }
        ]
        ```
        <br>
        **참고**<br>
        - timeMin, timeMax의 +는 반드시 %2B로 인코딩해야 합니다.
        - sourceType은 "calendar", "short-recommend", "long-recommend" 3가지
        """
    )

    @GetMapping("/events")
    public List<FormattedTimeSlot> getGoogleCalendarEvents(
            @RequestHeader("Authorization") String authorizationHeader,  // Authorization 헤더에서 JWT를 가져옴
            @RequestParam String calendarId,
            @RequestParam String timeMin,
            @RequestParam String timeMax) throws Exception {

        // Bearer 접두사 제거
        String jwtToken = jwtUtil.cleanBearer(authorizationHeader);  // Bearer " " 제거 후 JWT 얻기

        // JWT 토큰에서 이메일을 추출하여 User 객체를 가져옴
        String email = oAuthService.getGoogleUserEmailFromJwt(jwtToken);  // JWT에서 이메일을 추출
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 구글 캘린더 이벤트 조회
        return calendarService.getFormattedEvents(user, calendarId, timeMin, timeMax);
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
                req.userLabel,
                null
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