package com.example.Easeplan.api.HaruRecord.service;

import com.example.Easeplan.api.HaruRecord.domain.DailyEvaluation;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationRequest;
import com.example.Easeplan.api.HaruRecord.dto.DailyEvaluationResponse;
import com.example.Easeplan.api.HaruRecord.repository.DailyEvaluationRepository;
import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

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
    public void createEvaluation(User user, DailyEvaluationRequest request) {
        DailyEvaluation evaluation = DailyEvaluation.builder()
                .user(user)
                .date(request.getDate())
                .emotion(request.getEmotion())
                .fatigueLevel(request.getFatigueLevel())
                .weather(request.getWeather())
                .specialNotes(request.getSpecialNotes())
                .build();
        evaluationRepository.save(evaluation);
    }

}
