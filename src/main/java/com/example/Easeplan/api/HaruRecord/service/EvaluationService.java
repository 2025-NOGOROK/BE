package com.example.Easeplan.api.HaruRecord.service;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationRequest;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationResponse;
import com.example.Easeplan.api.HaruRecord.repository.DailyEvaluationRepository;
import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EvaluationService {
    private final DailyEvaluationRepository evaluationRepository;

    public EvaluationService(DailyEvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    public DailyEvaluationResponse getEvaluationByDate(User user, LocalDate date) {
        DailyEvaluation evaluation = evaluationRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new EntityNotFoundException("해당 날짜의 기록이 없습니다."));
        return DailyEvaluationResponse.from(evaluation);
    }

    // ★ 추가: 기록 저장 메서드
    @Transactional
    public void createEvaluation(User user, DailyEvaluationRequest request) {
        DailyEvaluation entity = evaluationRepository
                .findTopByUserAndDateOrderByIdDesc(user, request.getDate())
                .orElse(null);

        if (entity == null) {
            entity = DailyEvaluation.builder()
                    .user(user)
                    .date(request.getDate())
                    .build();
        }

        // 엔티티에 업데이트 메서드 하나 추가해서 깔끔하게 갱신
        entity.updateFrom(request);
        evaluationRepository.save(entity);
    }
}
