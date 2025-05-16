package com.example.Easeplan.api.ShortFlask.service;

import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class FlaskRecommendService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String flaskUrl;

    public FlaskRecommendService(@Value("${flask.short-reco}") String flaskUrl) {
        this.flaskUrl = flaskUrl;
    }



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

    // FlaskRecommendService.java
    public ResponseEntity<List> callFlaskApi(String url, Map<String, Object> body) {
        return restTemplate.postForEntity(url, body, List.class);
    }

    // FlaskRecommendService.java
    public List<String> getStressRecommendations(UserSurveyRequest request) {
        Map<String, Object> requestBody = Map.of(
                "schedule", request.getScheduleType(),
                "stress_level", calculateStressLevel(request),
                "methods", request.getStressReliefMethods()
        );

        ResponseEntity<List> response = restTemplate.postForEntity(
                "http://:5000/stress-analysis",
                requestBody,
                List.class
        );
        return response.getBody();
    }

    private Double calculateStressLevel(UserSurveyRequest request) {
        return switch (request.getStressReaction()) {
            case "anger" -> 80.0;
            case "depression" -> 75.0;
            default -> 50.0;
        };
    }

}
