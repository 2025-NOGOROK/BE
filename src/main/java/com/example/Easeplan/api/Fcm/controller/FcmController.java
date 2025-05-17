package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.domain.ScheduledNotification;
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
import com.example.Easeplan.api.Fcm.dto.NotificationScheduleRequest; // ✅ 올바른 DTO 임포트

import java.time.ZonedDateTime;

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

    // 1. FCM 토큰 등록
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



    // 3. 예약 알림 등록
    @Operation(
            summary = "예약 알림 등록",
            description = """
        사용자가 설정한 시간에 FCM 알림을 예약합니다.<br>
        <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
        
        <b>요청 본문 예시:</b>
        <pre>
{
  "title": "회의 시작 알림",
  "startDateTime": "2025-05-20T14:00:00+09:00",
  "minutesBeforeAlarm": 10
}
        </pre>

        <b>필드 설명:</b>
        - title: 알림 제목 (최대 100자) <b>[필수]</b><br>
        - startDateTime: 일정 시작 시간 (ISO 8601 형식, 예: 2025-05-20T14:00:00+09:00) <b>[필수]</b><br>
        - minutesBeforeAlarm: 알림을 보낼 시간(분 단위, 1~1440) <b>[필수]</b><br>

        <b>유효성 검사:</b>
        1. startDateTime은 현재 시간보다 미래여야 함
        2. minutesBeforeAlarm은 1 이상 1440 이하 정수
        
        <b>응답:</b>
        - 200 OK: 알림 예약 성공
        - 400 Bad Request: 유효하지 않은 입력 값
        - 401 Unauthorized: 인증 실패
        """
    )
    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody NotificationScheduleRequest request
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getFcmTokens().isEmpty()) {
            throw new RuntimeException("등록된 FCM 토큰이 없습니다");
        }
        String fcmToken = user.getFcmTokens().get(0);

        // 1. 알림 시간 계산
        ZonedDateTime notifyAt = request.startDateTime()
                .minusMinutes(request.minutesBeforeAlarm());

        // 2. 예약 알림 저장
        ScheduledNotification notification = new ScheduledNotification();
        notification.setTitle(request.title());
        notification.setFcmToken(fcmToken);
        notification.setNotifyAt(notifyAt);
        notificationRepo.save(notification);

        return ResponseEntity.ok("알림 예약 완료");
    }
    // 예약 알림 요청 DTO

}
