package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<HeartRate, Long> {
    // ✅ measuredAt 제거 → startTime으로 정렬
    Optional<HeartRate> findTopByUserEmailOrderByStartTimeDesc(String email);
    List<HeartRate> findTop1ByUserOrderByEndTimeDesc(User user);

}
