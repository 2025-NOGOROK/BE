package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmartwatchService {
    private final SmartwatchRepository smartwatchRepo;
    private final UserRepository userRepository;

    // 1시간마다 실행, 서버 시작 후 5초 뒤 첫 실행 (자동 저장을 위한 반복/조건 체크)
    @Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 5000)
    @Transactional
    public void saveHourlyHeartRate() {
        List<User> users = userRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        for (User user : users) {
            Optional<HeartRate> lastDataOpt = smartwatchRepo.findTopByUserEmailOrderByStartTimeDesc(user.getEmail());
            if (lastDataOpt.isPresent()) {
                HeartRate lastData = lastDataOpt.get();
                LocalDateTime lastStartTime = LocalDateTime.parse(lastData.getStartTime(), formatter);

                // 다음 저장할 startTime: 마지막 startTime의 시(minute, second, nano초 0) + 1시간
                LocalDateTime nextStartTime = lastStartTime.withMinute(0).withSecond(0).withNano(0).plusHours(1);
                LocalDateTime nextEndTime = nextStartTime.plusMinutes(30);

                // 현재 시간이 nextStartTime을 지났으면 저장
                if (!now.isBefore(nextStartTime)) {
                    HeartRateRequest request = new HeartRateRequest();
                    request.setEmail(user.getEmail());
                    request.setMin(lastData.getMin());     // 실제 데이터 수집 로직 필요
                    request.setMax(lastData.getMax());
                    request.setAvg(lastData.getAvg());
                    request.setStartTime(nextStartTime.format(formatter));
                    request.setEndTime(nextEndTime.format(formatter));
                    request.setCount(lastData.getCount());
                    request.setStress(lastData.getStress());
                    saveData(request); // 자기 자신 메서드 호출
                }
            }
        }
    }

    @Transactional
    public void saveData(HeartRateRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        HeartRate newData = HeartRate.builder()
                .user(user)
                .min(request.getMin())
                .max(request.getMax())
                .avg(request.getAvg())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .count(request.getCount())
                .stress(request.getStress())
                .build();

        smartwatchRepo.save(newData);
    }

//    @Transactional
//    public void updateDeviceData(HeartRateRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));
//
//        HeartRate data = smartwatchRepo.findTopByUserEmailOrderByStartTimeDesc(user.getEmail())
//                .orElseThrow(() -> new RuntimeException("해당 데이터가 없습니다."));
//
//        if (request.getMin() != null) data.setMin(request.getMin());
//        if (request.getMax() != null) data.setMax(request.getMax());
//        if (request.getAvg() != null) data.setAvg(request.getAvg());
//        if (request.getStress() != null) data.setStress(request.getStress());
//        if (request.getStartTime() != null) data.setStartTime(request.getStartTime());
//        if (request.getEndTime() != null) data.setEndTime(request.getEndTime());
//        if (request.getCount() != null) data.setCount(request.getCount());
//    }

    public Optional<Float> getClosestAvgHeartRate(User user) {
        LocalDateTime now = LocalDateTime.now();
        String todayStart = now.toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String todayEnd = now.toLocalDate().atTime(23,59,59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        List<HeartRate> todayRates = smartwatchRepo.findByUserAndStartTimeBetween(user, todayStart, todayEnd);

        if (todayRates.isEmpty()) return Optional.empty();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        return todayRates.stream()
                .min(Comparator.comparing(hr -> {
                    LocalDateTime hrTime = LocalDateTime.parse(hr.getStartTime(), formatter);
                    return Math.abs(java.time.Duration.between(hrTime, now).toSeconds());
                }))
                .map(HeartRate::getAvg);
    }
}
