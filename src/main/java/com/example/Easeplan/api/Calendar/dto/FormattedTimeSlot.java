package com.example.Easeplan.api.Calendar.dto;

import lombok.Getter;

@Getter
public class FormattedTimeSlot {
    private final String title;
    private final String description;
    private final String startTime;
    private final String endTime;
    private String sourceType;  // final을 제거하고 mutable로 변경

    // 생성자에서 값 주입
    public FormattedTimeSlot(String title, String description, String startTime, String endTime, String sourceType) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sourceType = sourceType;  // 초기화 시에 설정
    }

    public String getStartDateTime() {
        return startTime;
    }

    public String getEndDateTime() {
        return endTime;
    }

    // sourceType을 설정하는 setter 메서드 추가
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
