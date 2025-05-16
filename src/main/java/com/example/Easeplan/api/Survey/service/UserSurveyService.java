package com.example.Easeplan.api.Survey.service;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.repository.UserSurveyRepository;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserSurveyService {
    private final UserSurveyRepository repository;

    public UserSurveyService(UserSurveyRepository repository) {
        this.repository = repository;
    }


    // UserSurveyService.java
    @Transactional
    public void saveSurvey(User user, UserSurveyRequest request) {
        // 1. 기존 데이터 조회
        Optional<UserSurvey> existingSurvey = repository.findByUser(user);

        // 2. 존재하면 업데이트, 없으면 새로 생성
        if (existingSurvey.isPresent()) {
            updateExistingSurvey(existingSurvey.get(), request);
        } else {
            createNewSurvey(user, request);
        }
    }

    // 3. 기존 설문 업데이트 메서드 (private으로 분리)
    private void updateExistingSurvey(UserSurvey survey, UserSurveyRequest request) {
        List<String> methods = (request.getHasStressRelief() != null && request.getHasStressRelief())
                ? (request.getStressReliefMethods() != null ? request.getStressReliefMethods() : List.of())
                : List.of();

        survey.setScheduleType(request.getScheduleType());
        survey.setSuddenChangePreferred(request.getSuddenChangePreferred());
        survey.setChronotype(request.getChronotype());
        survey.setPreferAlone(request.getPreferAlone());
        survey.setStressReaction(request.getStressReaction());
        survey.setHasStressRelief(request.getHasStressRelief());
        survey.setStressReliefMethods(methods);
    }

    // 4. 새 설문 생성 메서드 (기존 로직 재사용)
    private void createNewSurvey(User user, UserSurveyRequest request) {
        List<String> methods = (request.getHasStressRelief() != null && request.getHasStressRelief())
                ? (request.getStressReliefMethods() != null ? request.getStressReliefMethods() : List.of())
                : List.of();

        UserSurvey survey = UserSurvey.builder()
                .user(user)
                .scheduleType(request.getScheduleType())
                .suddenChangePreferred(request.getSuddenChangePreferred())
                .chronotype(request.getChronotype())
                .preferAlone(request.getPreferAlone())
                .stressReaction(request.getStressReaction())
                .hasStressRelief(request.getHasStressRelief())
                .stressReliefMethods(methods)
                .build();

        repository.save(survey);
    }
    public UserSurvey getSurveyByUser(User user) {
        return repository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("설문 데이터가 없습니다."));
    }
}
