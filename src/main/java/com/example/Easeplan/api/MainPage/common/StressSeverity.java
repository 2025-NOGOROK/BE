package com.example.Easeplan.api.MainPage.common;

public enum StressSeverity {

    LOW,
    MODERATE,
    HIGH;

    public static StressSeverity ofTotal(int total){
        if(total<=7) return LOW;
        if (total <= 14) return MODERATE;
        return HIGH;
    }
}
