package com.example.Easeplan.api.Survey.dto;

import com.example.Easeplan.api.Survey.domain.UserSurvey;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserSurveyRequest {
    private String email; // 사용자 식별 이메일
    private String scheduleType;
    private Boolean suddenChangePreferred;
    private String chronotype;
    private String preferAlone;
    private String stressReaction;
    private Boolean hasStressRelief;
    private List<String> stressReliefMethods;

    // UserSurvey → UserSurveyRequest 변환
    public static UserSurveyRequest fromEntity(UserSurvey survey) {
        return new UserSurveyRequest(
                survey.getUser().getEmail(), // User 연관관계에서 email 추출
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
