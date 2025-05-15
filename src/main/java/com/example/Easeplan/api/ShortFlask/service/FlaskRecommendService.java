package com.example.Easeplan.api.ShortFlask.service;

import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class FlaskRecommendService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String flaskUrl = "http://3.39.65.123:5000/recommend";


    public List<String> getRecommendations(UserSurveyRequest request) {
        // ✅ 올바른 getter 메서드 사용
        Map<String, Object> requestBody = Map.of(
                "schedule", request.getScheduleType(),          // scheduleType → schedule
                "change", request.getSuddenChangePreferred(),   // suddenChangePreferred → change
                "time", request.getChronotype(),                // chronotype → time
                "social", request.getPreferAlone(),             // preferAlone → social
                "sensory", request.getStressReaction(),         // stressReaction → sensory
                "stress_input", request.getHasStressRelief() ? "O" : "X",
                "user_methods", request.getStressReliefMethods()
        );

        ResponseEntity<List> response = restTemplate.postForEntity(
                flaskUrl,
                requestBody,
                List.class
        );
        return response.getBody();
    }
}
