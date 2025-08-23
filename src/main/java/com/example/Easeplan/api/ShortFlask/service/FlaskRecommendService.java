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
        // 설문값이 이미 한글로 넘어오므로 그대로 전달
        Map<String, Object> requestBody = Map.of(
                "schedule", request.getScheduleType(),         // "루즈"/"타이트"
                "change", request.getSuddenChangePreferred(),  // true/false (Flask에서 O/X 처리)
                "time", request.getChronotype(),               // "아침"/"저녁"
                "social", request.getPreferAlone(),            // "혼자"/"함께"
                "sensory", request.getStressReaction(),        // 감각유형 필드면 그대로 (CSV와 일치해야 함)
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

    public ResponseEntity<List> callFlaskApi(String url, Map<String, Object> body) {
        return restTemplate.postForEntity(url, body, List.class);
    }

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
