package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class SmartwatchService {
    private final SmartwatchRepository smartwatchRepo;
    private final UserRepository userRepository;

    @Transactional
    public void saveData(HeartRateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        // 2. 엔티티 생성
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

        // 3. 저장
        smartwatchRepo.save(newData);
    }
    @Transactional
    public void updateDeviceData(HeartRateRequest request) {

        // ✅ measuredAt 제거, email로 사용자 조회 추가
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        HeartRate data = smartwatchRepo.findTopByUserEmailOrderByStartTimeDesc(user.getEmail())
                .orElseThrow(() -> new RuntimeException("해당 데이터가 없습니다."));

        // ✅ DTO에 없는 필드(heartRate 등) 제거
        if (request.getMin() != null) data.setMin(request.getMin());
        if (request.getMax() != null) data.setMax(request.getMax());
        if (request.getAvg() != null) data.setAvg(request.getAvg());
        if (request.getStress() != null) data.setStress(request.getStress());
        if (request.getStartTime() != null) data.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) data.setEndTime(request.getEndTime());
        if (request.getCount() != null) data.setCount(request.getCount());
    }



}





