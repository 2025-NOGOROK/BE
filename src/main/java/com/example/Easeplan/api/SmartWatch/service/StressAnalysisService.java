package com.example.Easeplan.api.SmartWatch.service;

import com.example.Easeplan.api.ShortFlask.service.FlaskRecommendService;
import com.example.Easeplan.api.Survey.domain.UserSurvey;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.repository.UserSurveyRepository;
import com.example.Easeplan.global.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// StressAnalysisService.java
@Service
@RequiredArgsConstructor
public class StressAnalysisService {
    private final FlaskRecommendService flaskService;
    private final UserSurveyRepository userSurveyRepo;

    public List<String> analyzeStress(User user) {
        UserSurvey survey = userSurveyRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("User has no survey data"));

        UserSurveyRequest request = UserSurveyRequest.fromEntity(survey);

        Map<String, Object> requestBody = Map.of(
                "schedule", request.getScheduleType(),
                "stress_level", calculateStressLevel(request),
                "methods", request.getStressReliefMethods()
        );

        ResponseEntity<List> response = flaskService.callFlaskApi(
                "",
                requestBody
        );
        return response.getBody();
    }

    private Object calculateStressLevel(UserSurveyRequest request) {
        return 0;
    }
}
