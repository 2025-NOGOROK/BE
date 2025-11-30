package com.example.Easeplan.api.Survey.service;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.repository.UserSurveyRepository;
import com.example.Easeplan.global.auth.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserSurveyService {
    private final UserSurveyRepository repository;

    public UserSurveyService(UserSurveyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveSurvey(User user, UserSurveyRequest request) {
        var existing = repository.findByUser(user);
        if (existing.isPresent()) {
            updateExistingSurvey(existing.get(), request);
        } else {
            createNewSurvey(user, request);
        }
    }

    private void updateExistingSurvey(UserSurvey survey, UserSurveyRequest request) {
        List<String> methods =
                (request.getHasStressRelief() != null && request.getHasStressRelief())
                        ? (request.getStressReliefMethods() != null ? request.getStressReliefMethods() : List.of())
                        : List.of();

        survey.updateSurvey(
                request.getScheduleType(),
                request.getSuddenChangePreferred(),
                request.getChronotype(),
                request.getPreferAlone(),
                request.getStressReaction(),
                request.getHasStressRelief(),
                methods
        );
        // @Transactional 안에서 변경분은 더티체킹으로 자동 반영
    }

    private void createNewSurvey(User user, UserSurveyRequest request) {
        List<String> methods =
                (request.getHasStressRelief() != null && request.getHasStressRelief())
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

    @Transactional(readOnly = true)
    public UserSurvey getSurveyByUser(User user) {
        return repository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("설문 데이터가 없습니다."));
    }
}
