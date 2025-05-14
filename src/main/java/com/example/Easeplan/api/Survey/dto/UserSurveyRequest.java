package com.example.Easeplan.api.Survey.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class UserSurveyRequest {
    private String scheduleType;
    private Boolean suddenChangePreferred;
    private String chronotype;
    private String preferAlone;
    private String stressReaction;
    private Boolean hasStressRelief;
    private List<String> stressReliefMethods;
}
