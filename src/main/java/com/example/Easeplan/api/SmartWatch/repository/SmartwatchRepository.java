package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<HeartRate, Long> {
    // ✅ measuredAt 제거 → startTime으로 정렬
    Optional<HeartRate> findTopByUserEmailOrderByStartTimeDesc(String email);
}
