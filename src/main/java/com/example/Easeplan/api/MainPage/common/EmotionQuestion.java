package com.example.Easeplan.api.MainPage.common;

public enum EmotionQuestion {
    Q1("Irritable"),
    Q2("Tense"),
    Q3("Overwhelmed"),
    Q4("MoodSwings"),
    Q5("StressSleep"),
    Q6("EmotionRegHard"),
    Q7("NoSupport");

    private final String shortEn;
    EmotionQuestion(String shortEn){ this.shortEn=shortEn;}
    public String shortEn(){
        return shortEn;
    }

}
