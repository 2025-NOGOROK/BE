package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
import com.example.Easeplan.api.Fcm.dto.NotificationScheduleRequest;
import com.example.Easeplan.api.Fcm.repository.ScheduledNotificationRepository;
import com.example.Easeplan.api.Fcm.service.FcmService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    // 1) FCM 토큰 등록 (+ 유효성 검증)
    @Operation(
            summary = "FCM 토큰 등록(+유효성 검증)",
            description = "토큰을 FCM dry-run으로 검증한 뒤, 유효하면 사용자 계정에 저장합니다. "
                    + "기본적으로 validate=true이며, 무효 토큰이면 422(Unprocessable Entity)로 이유를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "등록 성공(유효성 검증 통과)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "성공",
                                    value = """
                {
                  "registered": true,
                  "validated": true,
                  "validation": {
                    "ok": true,
                    "dryRun": true,
                    "messageId": "projects/<proj>/messages/xxxxx"
                  }
                }
                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "무효 토큰(UNREGISTERED/INVALID_ARGUMENT 등) — 저장하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(name = "무효 토큰(UNREGISTERED)",
                                    value = """
                {
                  "registered": false,
                  "validated": true,
                  "validation": {
                    "ok": false,
                    "dryRun": true,
                    "code": "UNREGISTERED",
                    "errorCode": "NOT_FOUND",
                    "msg": "Requested entity was not found.",
                    "httpStatus": 404,
                    "httpBody": "{\\n  \\"error\\": {\\n    \\"code\\": 404,\\n    \\"message\\": \\"Requested entity was not found.\\",\\n    \\"status\\": \\"NOT_FOUND\\",\\n    \\"details\\": [\\n      {\\n        \\"@type\\": \\"type.googleapis.com/google.firebase.fcm.v1.FcmError\\",\\n        \\"errorCode\\": \\"UNREGISTERED\\"\\n      }\\n    ]\\n  }\\n}\\n"
                  }
                }
                """
                            )
                    )
            )})
        @PostMapping("/register")
    public ResponseEntity<?> registerFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String token,
            @RequestParam(defaultValue = "true") boolean validate
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> validation = null;
        if (validate) {
            validation = fcmService.validateToken(token);
            if (!Boolean.TRUE.equals(validation.get("ok"))) {
                // 저장하지 않고 422 + 실패사유 반환
                return ResponseEntity.unprocessableEntity().body(
                        Map.of(
                                "registered", false,
                                "validated", true,
                                "validation", validation
                        )
                );
            }
        }

        // 유효하면 저장 (idempotent 가정)
        user.addFcmToken(token);
        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of(
                        "registered", true,
                        "validated", validate,
                        "validation", validation
                )
        );
    }

    // 2) 예약 알림 등록
    @Operation(summary = "예약 알림 등록", description = """
        사용자가 설정한 시간에 FCM 알림을 예약합니다.<br>
        <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
        요청 본문:
        {
          "title": "회의 시작 알림",
          "startDateTime": "2025-05-20T14:00:00+09:00",
          "minutesBeforeAlarm": 10
        }""")
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NotificationScheduleRequest request
    ) {
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

    // 3) 단건 테스트 발송/검증 (dryRun=true면 유효성만)
    @Operation(summary = "테스트 FCM 발송(또는 유효성 검사)")
    @GetMapping("/test-send")
    public ResponseEntity<?> testSendDebug(@RequestParam String token,
                                           @RequestParam(defaultValue = "false") boolean dryRun) {
        Map<String, Object> out = fcmService.sendMessageDebug(
                token, "백엔드 테스트", "debug send", Map.of("type", "DEBUG"), dryRun);
        return Boolean.TRUE.equals(out.get("ok")) ? ResponseEntity.ok(out)
                : ResponseEntity.status(502).body(out);
    }

    // (선택) 전용 유효성 체크
    @Operation(summary = "FCM 토큰 유효성 검사(dry-run)")
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String token) {
        return ResponseEntity.ok(fcmService.validateToken(token));
    }
}
