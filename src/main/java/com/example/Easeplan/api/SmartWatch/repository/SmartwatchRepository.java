package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.service.HeartRateAnalyzer;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<HeartRate, Long> {
    // ✅ measuredAt 제거 → startTime으로 정렬
    Optional<HeartRate> findTopByUserEmailOrderByStartTimeDesc(String email);

    //최근 시간
    @Query("SELECT h FROM HeartRate h WHERE h.user = :user AND h.startTime >= :startTime AND h.startTime <= :endTime")
    List<HeartRate> findByUserAndStartTimeBetween(
            @Param("user") User user,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime
    );
    // 해당 월에 데이터 존재 여부
    boolean existsByUserEmailAndStartTimeStartingWith(String email, String monthPrefix);

    List<HeartRate> findByUserEmailAndStartTimeStartingWith(String email, String dayPrefix);


}
