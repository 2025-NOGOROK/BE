package com.example.Easeplan.api.Fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StressFcmload {
    private String type;     // "EMERGENCY_STRESS"
    private String title;    // 알림 타이틀
    private String body;     // 알림 본문
    private String eventId;  // 어떤 이벤트를 활성화할지 식별
}
