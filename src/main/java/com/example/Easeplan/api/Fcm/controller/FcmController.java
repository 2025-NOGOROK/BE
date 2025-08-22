package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import com.example.Easeplan.api.Fcm.dto.NotificationScheduleRequest;
import com.example.Easeplan.api.Fcm.repository.ScheduledNotificationRepository;
import com.example.Easeplan.api.Fcm.service.FcmService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Map;

@Tag(name = "FCM", description = "FCM API")
@RestController
@RequestMapping("/api/fcm")
public class FcmController {
    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final ScheduledNotificationRepository notificationRepo;

    public FcmController(
            FcmService fcmService,
            UserRepository userRepository,
            ScheduledNotificationRepository notificationRepo
    ) {
        this.fcmService = fcmService;
        this.userRepository = userRepository;
        this.notificationRepo = notificationRepo;
    }

    // 1) FCM 토큰 등록
    @Operation(summary = "FCM 토큰 등록", description = "사용자의 FCM 토큰을 서버에 저장합니다.")
    @PostMapping("/register")
    public ResponseEntity<Void> registerFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String token
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.addFcmToken(token);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // 2) 예약 알림 등록 (세터 없이 저장)
    @Operation(
            summary = "예약 알림 등록",
            description = """
        사용자가 설정한 시간에 FCM 알림을 예약합니다.<br>
        <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
        요청 본문:
        {
          "title": "회의 시작 알림",
          "startDateTime": "2025-05-20T14:00:00+09:00",
          "minutesBeforeAlarm": 10
        }"""
    )
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NotificationScheduleRequest request
    ) {
        // 기본 검증
        if (request.title() == null || request.title().isBlank()) {
            return ResponseEntity.badRequest().body("title은 필수입니다.");
        }
        if (request.minutesBeforeAlarm() < 1 || request.minutesBeforeAlarm() > 1440) {
            return ResponseEntity.badRequest().body("minutesBeforeAlarm은 1~1440 사이여야 합니다.");
        }
        if (request.startDateTime() == null) {
            return ResponseEntity.badRequest().body("startDateTime은 필수입니다.");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getFcmTokens() == null || user.getFcmTokens().isEmpty()) {
            return ResponseEntity.badRequest().body("등록된 FCM 토큰이 없습니다.");
        }
        String fcmToken = user.getFcmTokens().get(0);

        ZonedDateTime now = ZonedDateTime.now(request.startDateTime().getZone());
        if (!request.startDateTime().isAfter(now)) {
            return ResponseEntity.badRequest().body("startDateTime은 현재보다 미래여야 합니다.");
        }

        ZonedDateTime notifyAt = request.startDateTime().minusMinutes(request.minutesBeforeAlarm());
        if (!notifyAt.isAfter(now)) {
            return ResponseEntity.badRequest().body("알림 시간이 현재보다 이후가 되도록 minutesBeforeAlarm을 조정하세요.");
        }

        // 세터 없이 빌더로 생성
        ScheduledNotification notification = ScheduledNotification.builder()
                .title(request.title())
                .fcmToken(fcmToken)
                .notifyAt(notifyAt)
                .build();

        notificationRepo.save(notification);

        return ResponseEntity.ok(
                Map.of(
                        "message", "알림 예약 완료",
                        "id", notification.getId(),
                        "notifyAt", notifyAt.toString()
                )
        );
    }

    // 3) 단건 테스트 발송
    @Operation(summary = "테스트 FCM 발송")
    @GetMapping("/test-send")
    public ResponseEntity<?> testSend(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String token
    ) {
        boolean ok = fcmService.sendMessage(token, "백엔드 테스트", "서버에서 FCM 보내기 성공!");
        return ok ? ResponseEntity.ok("FCM 전송 성공")
                : ResponseEntity.status(502).body("FCM 전송 실패");
    }
}
