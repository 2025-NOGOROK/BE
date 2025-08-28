package com.example.Easeplan.api.HaruRecord.repository;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEvaluationRepository extends JpaRepository<DailyEvaluation, Long> {
    Optional<DailyEvaluation> findByUserAndDate(User user, LocalDate date);

    //startDate 이상, endDate 이하인 모든 DailyEvaluation을 조회
    List<DailyEvaluation> findAllByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
    // 같은 날짜에 여러 건 있어도 최신(id 큰 것) 1건만 반환
    Optional<DailyEvaluation> findTopByUserAndDateOrderByIdDesc(User user, LocalDate date);

}
