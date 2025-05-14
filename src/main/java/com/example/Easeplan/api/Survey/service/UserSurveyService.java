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

        // null-safe하게 처리
        List<String> methods = (request.getHasStressRelief() != null && request.getHasStressRelief())
                ? (request.getStressReliefMethods() != null ? request.getStressReliefMethods() : List.of())
                : List.of();
        UserSurvey survey = UserSurvey.builder()
                .user(user)
                .scheduleType(request.getScheduleType())          // ✅ getScheduleType()
                .suddenChangePreferred(request.getSuddenChangePreferred())
                .chronotype(request.getChronotype())
                .preferAlone(request.getPreferAlone())
                .stressReaction(request.getStressReaction())
                .hasStressRelief(request.getHasStressRelief())
                .stressReliefMethods(methods)   // ← 여기에 methods 사용!
                .build();
        repository.save(survey);
    }
}
