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

import java.time.*;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmergencyStressService {

    private static final int WINDOW_DAYS = 14;      // 최근 14일
    private static final int MIN_HIGH_DAYS = 11;    // 그 중 11일 이상
    private static final double THRESHOLD = 70.0;   // stressEma 임계값(0~100)

    private final UserRepository userRepository;
    private final SmartwatchRepository smartwatchRepository;
    private final FcmService fcmService;
    private final EmergencyStressEventRepository eventRepository;

    // 매일 01:00 (KST) 실행
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    @Transactional
    public void evaluateAllUsersAndTrigger() {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(KST);
        LocalDate endDate = today.minusDays(1); // 오늘 제외, 어제까지

        for (User user : userRepository.findAll()) {
            if (!user.isPushNotificationAgreed()) continue;

            int highDays = countHighStressDays(user, endDate, KST);
            if (highDays < MIN_HIGH_DAYS || !shouldSendFcm(user, today)) continue;

            boolean hasPending = eventRepository
                    .findTopByUserAndStatusOrderByCreatedAtDesc(user, EmergencyStressEvent.Status.PENDING)
                    .isPresent();
            if (hasPending) continue;

            // 1) 이벤트 생성(PENDING)
            var event = EmergencyStressEvent.builder()
                    .user(user)
                    .status(EmergencyStressEvent.Status.PENDING)
                    .createdAt(LocalDateTime.now(KST))
                    .build();
            eventRepository.save(event);

            // 2) FCM 발송
            if (user.getFcmTokens() != null) {
                for (String token : user.getFcmTokens()) {
                    try {
                        fcmService.sendEmergencyStressAsk(token, event.getId());
                    } catch (Exception ignored) {}
                }
            }

            // 3) 2주 재전송 방지
            user.markAutoBreakFcmSentAt(today);
            userRepository.save(user);
        }
    }

    private int countHighStressDays(User user, LocalDate endDate, ZoneId zone) {
        int highDays = 0;
        for (int i = 0; i < WINDOW_DAYS; i++) {
            LocalDate target = endDate.minusDays(i);

            long dayStartMs = target.atStartOfDay(zone).toInstant().toEpochMilli();
            long dayEndMsEx = target.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(); // exclusive

            List<HeartRate> day = smartwatchRepository
                    .findByUserEmailAndTimestampBetween(user.getEmail(), dayStartMs, dayEndMsEx - 1);

            if (day.isEmpty()) continue;

            double dailyAvg = day.stream()
                    .map(HeartRate::getStressEma)           //  avg → stressEma
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(Double.NaN);

            if (!Double.isNaN(dailyAvg) && dailyAvg >= THRESHOLD) {
                highDays++;
            }
        }
        return highDays;
    }

    private boolean shouldSendFcm(User user, LocalDate today) {
        return user.getLastAutoBreakFcmSentAt() == null
                || user.getLastAutoBreakFcmSentAt().isBefore(today.minusDays(14));
    }
}
