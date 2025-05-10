package com.example.Easeplan.api.Evaluation.repository;

import com.example.Easeplan.api.Evaluation.domain.ScheduleFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleFeedbackRepository extends JpaRepository<ScheduleFeedback, Long> {


    // 전체 일정 개수 (기간 내)
    @Query("SELECT COUNT(sf) FROM ScheduleFeedback sf WHERE sf.feedbackAt BETWEEN :start AND :end")
    long countTotalByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 완료 일정 개수 (기간 내)
    @Query("SELECT COUNT(sf) FROM ScheduleFeedback sf WHERE sf.completed = true AND sf.feedbackAt BETWEEN :start AND :end")
    long countCompletedByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 일정 사유
    @Query("SELECT sf.reason, COUNT(sf) FROM ScheduleFeedback sf " +
            "WHERE sf.completed = false AND sf.feedbackAt BETWEEN :start AND :end " +
            "GROUP BY sf.reason")
    List<Object[]> countByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(sf) FROM ScheduleFeedback sf " +
            "WHERE sf.completed = false AND sf.feedbackAt BETWEEN :start AND :end")
    long countIncomplete(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
