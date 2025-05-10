package com.example.Easeplan.api.HaruRecord.domain;

public enum Emotion {
    JOY("기쁨"),
    NORMAL("보통"),
    DEPRESSED("우울"),
    IRRITATED("짜증"),
    ANGRY("화남");

    private final String description;


    // 생성자 선언
    Emotion(String description) {
        this.description = description;
    }

    // getter 추가
    public String getDescription() {
        return description;
    }
}




