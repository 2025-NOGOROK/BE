package com.example.Easeplan.api.Recommend.Long.repository;

import com.example.Easeplan.api.Recommend.Long.dto.UserChoice;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserChoiceRepository extends JpaRepository<UserChoice, Long> {
    // 전날 type="event" 긴 추천만 조회
    // 어제 날짜 계산
    List<UserChoice> findByUserAndTypeAndStartTimeBetween(
            User user, String type, String startTime, String endTime
    );

    Optional<UserChoice> findTopByUserAndTypeOrderByStartTimeDesc(User user, String type);


}
