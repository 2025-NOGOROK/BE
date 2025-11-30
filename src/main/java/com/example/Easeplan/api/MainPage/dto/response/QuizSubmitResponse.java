package com.example.Easeplan.api.MainPage.dto.response;

import java.util.Map;

public record QuizSubmitResponse (
        int totalScore,
        String severity,
        Map<String, Integer> perItemScore
){

}
