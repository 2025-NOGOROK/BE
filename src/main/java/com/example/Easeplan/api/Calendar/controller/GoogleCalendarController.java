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
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;

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

    private final JwtUtil jwtProvider;

    public GoogleCalendarController(GoogleOAuthService oAuthService, GoogleCalendarService calendarService, NotificationScheduler notificationScheduler, UserRepository userRepository, JwtUtil jwtProvider) {
        this.oAuthService = oAuthService;
        this.calendarService = calendarService;
        this.notificationScheduler = notificationScheduler;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    // 주의: 중첩된 경로 제거
//    @Operation(
//            summary = "Google OAuth 토큰 기반 모바일 회원가입 및 JWT 발급",
//            description = """
//        Android 앱에서 Google 로그인을 완료한 후 발급받은 `access_token`과 `refresh_token`을 서버에 전달하여 회원가입 및 JWT 발급을 수행합니다.<br><br>
//
//        ✅ **처리 흐름**<br>
//        - Google 로그인 완료 후 받은 access_token을 이용해 사용자 이메일을 확인합니다.<br>
//        - 이미 가입된 이메일이면 가입 거절(409 Conflict).<br>
//        - 신규 이메일인 경우 User 엔티티 생성 및 저장 후, 자체 서비스용 JWT 발급.<br><br>
//
//        📥 **요청 예시(JSON):**
//        ```json
//        {
//          "access_token": "ya29.a0AVvZV...",
//          "refresh_token": "1//0g7ZxV..."
//        }
//        ```
//
//        📤 **응답 예시(JSON):**
//        ```json
//        {
//          "message": "회원가입 및 로그인 완료",
//          "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
//        }
//        ```
//
//        ❗ **주의사항:**<br>
//        - 프론트엔드(Android 앱)는 반드시 Google 로그인을 통해 `access_token`과 `refresh_token`을 먼저 획득해야 합니다.<br>
//        - 회원가입 API이므로, 로그인된 JWT 인증 없이도 호출 가능합니다.<br>
//        - 이 API는 최초 가입/등록에만 사용하고, 이후 로그인은 별도 로그인 API로 처리해야 합니다.
//    """
//    )
//
//    @PostMapping("/mobile-register")
//    public ResponseEntity<?> registerWithGoogleTokens(@RequestBody Map<String, String> request) {
//        try {
//            String accessToken = request.get("access_token");
//            String refreshToken = request.get("refresh_token");
//
//            if (accessToken == null || accessToken.isBlank()) {
//                return ResponseEntity.badRequest().body("access_token은 필수입니다.");
//            }
//
//            // ⬇️ access_token으로 사용자 정보 조회
//            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
//            String email = (String) userInfo.get("email");
//
//            if (email == null || email.isBlank()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 Google 계정입니다.");
//            }
//
//            // ⬇️ 이메일 중복 확인
//            if (userRepository.findByEmail(email).isPresent()) {
//                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 가입된 이메일입니다.");
//            }
//
//            // ⬇️ 유저 저장
//            User user = User.builder()
//                    .email(email)
//                    .googleAccessToken(accessToken)
//                    .googleRefreshToken(refreshToken)
//                    .googleAccessTokenExpiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(3600), ZoneOffset.UTC))
//                    .build();
//
//            userRepository.save(user);
//
//            // ⬇️ JWT 발급
//            String jwt = jwtProvider.createToken(user.getEmail()); // 여기서 getEmail() 명시
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "회원가입 및 로그인 완료",
//                    "token", jwt
//            ));
//        } catch (Exception e) {
//            log.error("Google 회원가입 실패", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류 발생");
//        }
//    }




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
    public ResponseEntity<?> googleCallback(@RequestParam String code) {
        try {
            // 1. 구글 인증 코드로 액세스 토큰과 리프레시 토큰을 받음
            Map<String, Object> tokenResponse = oAuthService.exchangeCodeForToken(code);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            // 2. 구글 사용자 정보 조회
            Map<String, Object> userInfo = oAuthService.getGoogleUserInfo(accessToken);
            String email = (String) userInfo.get("email");

            // 3. 사용자 정보 업데이트 또는 새로 생성
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. 받은 code를 DB에 저장 (Optional)
            user.setGoogleAuthCode(code);  // 구글 OAuth 코드 저장

            // 5. 토큰과 만료 시각 갱신
            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(3600); // 예시: 1시간 후 만료
            user.updateGoogleTokens(accessToken, refreshToken, newExpiresAt);

            // 6. DB에 저장
            userRepository.save(user);  // DB에 저장

            // 7. JWT 발급
            String jwtToken = jwtProvider.createToken(user.getEmail());

            // 8. 딥링크 URL 생성 (JWT를 앱으로 리디렉션)
            String redirectUri = "com.example.nogorok:/oauth2callback?jwt=" + jwtToken;

            // 9. 302 리디렉션 응답 (딥링크 URI로 리디렉션)
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUri))  // 딥링크 URI로 리디렉션
                    .build();

        } catch (Exception e) {
            log.error("구글 인증 처리 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth2 처리 중 오류 발생");
        }
    }










    @Operation(summary = "Google access_token 갱신", description = """
        사용자의 Google refresh_token을 사용하여 새로운 access_token을 갱신합니다.
        """)
    @PostMapping("/refresh-access-token")
    public ResponseEntity<?> refreshAccessToken(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = oAuthService.getOrRefreshGoogleAccessToken(user);

            return ResponseEntity.ok(Map.of(
                    "access_token", newAccessToken,
                    "expires_at", user.getGoogleAccessTokenExpiresAt()
            ));
        } catch (Exception e) {
            log.error("Google access_token 갱신 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google access_token 갱신 실패: 재로그인 필요");
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