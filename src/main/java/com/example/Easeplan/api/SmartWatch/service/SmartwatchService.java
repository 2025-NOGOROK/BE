package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import com.example.Easeplan.api.SmartWatch.dto.SmartwatchRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SmartwatchService {
    private final SmartwatchRepository smartwatchRepo;
    @Transactional
    public void saveData(SmartwatchRequest request) {
        // 1. deviceId로 기기 정보 조회 (getDeviceId() → deviceId())
        SmartwatchData registeredDevice = smartwatchRepo.findFirstByDeviceId(request.deviceId())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 기기입니다"));

        // 2. 새 데이터 생성 (getDeviceId() → deviceId())
        SmartwatchData newData = SmartwatchData.builder()
                .user(registeredDevice.getUser())
                .deviceId(request.deviceId())
                .stressIndex(request.stressIndex())
                .heartRate(request.heartRate())
                .temperature(request.temperature())
                .measuredAt(LocalDateTime.now())
                .build();

        smartwatchRepo.save(newData);
    }

    // 장치 연결 로직
    @Transactional
    public void connectDevice(User user, SmartwatchRequest request) {
        validateDuplicateDevice(user, request.deviceId());

        SmartwatchData data = SmartwatchData.builder()
                .user(user)
                .deviceId(request.deviceId())
                .stressIndex(request.stressIndex())
                .measuredAt(LocalDateTime.now())
                .heartRate(request.heartRate())
                .temperature(request.temperature())
                .build();

        smartwatchRepo.save(data);
    }

    // 중복 장치 검증
    private void validateDuplicateDevice(User user, String deviceId) {
        smartwatchRepo.findByUserEmail(user.getEmail()).stream()
                .filter(data -> data.getDeviceId().equals(deviceId))
                .findAny()
                .ifPresent(data -> {
                    throw new IllegalStateException("이미 등록된 기기입니다");
                });
    }

    @Transactional
    public List<SmartwatchData> getDeviceData(User user) {
        return smartwatchRepo.findByUserEmail(user.getEmail());
    }
}
