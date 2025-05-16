package com.example.Easeplan.api.Survey.dto;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
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

    // ✅ static 메서드로 변경 + Lombok @AllArgsConstructor와 호환되도록 수정
    public static UserSurveyRequest fromEntity(UserSurvey survey) {
        return new UserSurveyRequest(
                survey.getScheduleType(),
                survey.getSuddenChangePreferred(),
                survey.getChronotype(),
                survey.getPreferAlone(),
                survey.getStressReaction(),
                survey.getHasStressRelief(),
                survey.getStressReliefMethods()
        );
    }
}
