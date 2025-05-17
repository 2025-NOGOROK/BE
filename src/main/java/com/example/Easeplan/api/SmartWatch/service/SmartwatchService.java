package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import com.example.Easeplan.api.SmartWatch.dto.SmartwatchRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SmartwatchService {
    private final SmartwatchRepository smartwatchRepo;
    private final UserRepository userRepository;
    @Transactional
    public void saveData(SmartwatchRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        // 2. deviceId로 기기 정보 조회 (기기 등록 여부 확인)
        // 필요하다면 deviceId 중복 체크 로직 추가

        // 3. 새 데이터 생성
        SmartwatchData newData = SmartwatchData.builder()
                .user(user)
                .deviceId(request.deviceId())
                .measuredAt(request.timestamp() != null ? LocalDateTime.parse(request.timestamp()) : LocalDateTime.now())
                .min(request.min())
                .max(request.max())
                .avg(request.avg())
                .stress(request.stress())
                .heartRate(request.heartRate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .totalMinutes(request.totalMinutes())
                .bloodOxygen(request.bloodOxygen())
                .skinTemperature(request.skinTemperature())
                .build();

        smartwatchRepo.save(newData);
    }

    @Transactional
    public void updateDeviceData(User user, SmartwatchRequest request) {
        // 1. 해당 사용자의 해당 deviceId 데이터 조회 (여기선 가장 최근 데이터만 예시)
        SmartwatchData data = smartwatchRepo.findFirstByDeviceIdAndUserEmailOrderByMeasuredAtDesc(
                request.deviceId(), user.getEmail()
        ).orElseThrow(() -> new RuntimeException("해당 데이터가 없습니다."));

        // 2. 데이터 수정
        if(request.timestamp() != null)
            data.setMeasuredAt(LocalDateTime.parse(request.timestamp()));
        if(request.min() != null)
            data.setMin(request.min());
        if(request.max() != null)
            data.setMax(request.max());
        if(request.avg() != null)
            data.setAvg(request.avg());
        if(request.stress() != null)
            data.setStress(request.stress());
        if(request.heartRate() != null)
            data.setHeartRate(request.heartRate());
        if(request.startTime() != null)
            data.setStartTime(request.startTime());
        if(request.endTime() != null)
            data.setEndTime(request.endTime());
        if(request.totalMinutes() != null)
            data.setTotalMinutes(request.totalMinutes());
        if(request.bloodOxygen() != null)
            data.setBloodOxygen(request.bloodOxygen());
        if(request.skinTemperature() != null)
            data.setSkinTemperature(request.skinTemperature());

        // JPA의 Dirty Checking에 의해 자동 저장됨 (별도 save 불필요)
    }


}
