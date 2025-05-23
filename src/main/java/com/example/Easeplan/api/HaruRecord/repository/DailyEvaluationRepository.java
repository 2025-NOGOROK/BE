package com.example.Easeplan.api.HaruRecord.repository;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEvaluationRepository extends JpaRepository<DailyEvaluation, Long> {
    Optional<DailyEvaluation> findByUserAndDate(User user, LocalDate date);

    //월,일요일 해당하는 날짜
    List<DailyEvaluation> findAllByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
