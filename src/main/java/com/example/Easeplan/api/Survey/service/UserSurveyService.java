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
        UserSurvey survey = UserSurvey.builder()
                .user(user)  // user 객체를 넣어야 함
                .scheduleType(request.scheduleType())
                .suddenChangePreferred(request.suddenChangePreferred())
                .chronotype(request.chronotype())
                .preferAlone(request.preferAlone())
                .stressReaction(request.stressReaction())
                .hasStressRelief(request.hasStressRelief())
                .stressReliefMethods(
                        request.hasStressRelief() != null && request.hasStressRelief() ? request.stressReliefMethods() : List.of()
                )
                .build();
        repository.save(survey);
    }
}
