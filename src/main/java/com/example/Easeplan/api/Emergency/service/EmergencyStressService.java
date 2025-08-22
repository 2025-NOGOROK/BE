package com.example.Easeplan.api.Emergency.service;

import com.example.Easeplan.api.Emergency.domain.EmergencyStressEvent;
import com.example.Easeplan.api.Emergency.repository.EmergencyStressEventRepository;
import com.example.Easeplan.api.Fcm.service.FcmService;
import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
public class EmergencyStressService {

    private static final int WINDOW_DAYS = 14;
    private static final int MIN_HIGH_DAYS = 11;
    private static final double THRESHOLD = 70.0;

    private final UserRepository userRepository;
    private final SmartwatchRepository smartwatchRepository;
    private final FcmService fcmService;
    private final EmergencyStressEventRepository eventRepository;

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul") //1h
    @Transactional
    public void evaluateAllUsersAndTrigger() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);

        for (User user : userRepository.findAll()) {
            if (!user.isPushNotificationAgreed()) continue;

            int highDays = countHighStressDays(user, endDate);
            if (highDays < MIN_HIGH_DAYS || !shouldSendFcm(user, today)) continue;

            // 이미 PENDING 이벤트가 있다면 중복 생성 방지(선택)
            boolean hasPending = eventRepository
                    .findTopByUserAndStatusOrderByCreatedAtDesc(user, EmergencyStressEvent.Status.PENDING)
                    .isPresent();
            if (hasPending) continue;

            // 1) 이벤트 생성(PENDING)
            var event = EmergencyStressEvent.builder()
                    .user(user)
                    .status(EmergencyStressEvent.Status.PENDING)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            eventRepository.save(event);

            // 2) FCM 발송 (활성화 여부 묻기)
            if (user.getFcmTokens() != null) {
                for (String token : user.getFcmTokens()) {
                    try { fcmService.sendEmergencyStressAsk(token, event.getId()); } catch (Exception ignored) {}
                }
            }

            // 3) 2주 재전송 방지
            user.setLastAutoBreakFcmSentAt(today);
            userRepository.save(user);
        }
    }

    private int countHighStressDays(User user, LocalDate endDate) {
        int highDays = 0;
        for (int i = 0; i < WINDOW_DAYS; i++) {
            LocalDate target = endDate.minusDays(i);
            var day = smartwatchRepository.findByUserAndStartTimeBetween(
                    user,
                    target.atStartOfDay().format(F),
                    target.atTime(23, 59, 59).format(F)
            );

            if (day.isEmpty()) continue;

            double dailyAvg = day.stream()
                    .filter(hr -> hr.getAvg() != null)
                    .mapToDouble(HeartRate::getAvg)
                    .average()
                    .orElse(0.0);

            if (dailyAvg >= THRESHOLD) highDays++;
        }
        return highDays;
    }

    private boolean shouldSendFcm(User user, LocalDate today) {
        return user.getLastAutoBreakFcmSentAt() == null
                || user.getLastAutoBreakFcmSentAt().isBefore(today.minusDays(14));
    }
}