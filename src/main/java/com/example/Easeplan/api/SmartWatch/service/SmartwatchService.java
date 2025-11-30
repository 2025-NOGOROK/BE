package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.repository.SmartwatchRepository;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmartwatchService {

    private final SmartwatchRepository smartwatchRepo;
    private final UserRepository userRepository;

    /** 클라이언트가 보낸 원시 샘플 리스트 저장 (단일도 samples 1개로 전송) */
    @Transactional
    public void saveData(HeartRateRequest request) {
        if (request.getEmail() == null || request.getSamples() == null || request.getSamples().isEmpty()) {
            throw new IllegalArgumentException("email과 samples는 필수입니다.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다"));

        List<HeartRate> toSave = request.getSamples().stream()
                .map(s -> HeartRate.builder()
                        .user(user)
                        .timestamp(s.getTimestamp())
                        .heartRate(s.getHeartRate())
                        .rmssd(s.getRmssd())
                        .stressEma(s.getStressEma())
                        .stressRaw(s.getStressRaw())
                        .build())
                .toList();

        smartwatchRepo.saveAll(toSave);
    }

    /** 가장 최신 원시 샘플의 stressEma 반환 */
    public Optional<Double> getLatestStressEma(User user) {
        return smartwatchRepo.findTopByUserEmailOrderByTimestampDesc(user.getEmail())
                .map(HeartRate::getStressEma);
    }
}
