package com.example.Easeplan.api.Survey.dto;

import java.util.List;

public record UserSurveyRequest(
        String scheduleType,
        Boolean suddenChangePreferred,
        String chronotype,
        String preferAlone,
        String stressReaction,
        Boolean hasStressRelief,
        List<String> stressReliefMethods
) {}
