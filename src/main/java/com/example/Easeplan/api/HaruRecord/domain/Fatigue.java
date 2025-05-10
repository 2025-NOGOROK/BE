package com.example.Easeplan.api.HaruRecord.domain;

public enum Fatigue {
    VERY_TIRED("매우 피곤"),
    NORMAL("보통"),
    ENERGETIC("에너지 넘침");

    private final String description;


    // 생성자 선언
    Fatigue(String description) {
        this.description = description;
    }

    // getter 추가
    public String getDescription() {
        return description;
    }
}