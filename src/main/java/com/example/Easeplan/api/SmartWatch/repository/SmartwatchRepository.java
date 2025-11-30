package com.example.Easeplan.api.SmartWatch.repository;

import com.example.Easeplan.api.SmartWatch.domain.HeartRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SmartwatchRepository extends JpaRepository<HeartRate, Long> {

    Optional<HeartRate> findTopByUserEmailOrderByTimestampDesc(String email);

    List<HeartRate> findByUserEmailAndTimestampBetween(String email, Long startInclusive, Long endInclusive);

    boolean existsByUserEmailAndTimestampBetween(String email, Long startInclusive, Long endInclusive);
}
