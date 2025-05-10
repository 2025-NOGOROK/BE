package com.example.Easeplan.api.HaruRecord.domain;

public enum Weather {
    ICE("얼음"),
    CLOUDY("흐림"),
    RAIN("비"),
    SNOW("눈");

    private final String description;


    // 생성자 선언
    Weather(String description) {
        this.description = description;
    }

    // getter 추가
    public String getDescription() {
        return description;
    }
}