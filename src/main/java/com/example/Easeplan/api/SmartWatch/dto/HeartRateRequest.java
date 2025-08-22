package com.example.Easeplan.api.SmartWatch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HeartRateRequest {
    private String email;
    private List<HeartRateSample> samples;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeartRateSample {
        private Long timestamp;
        private Integer heartRate;
        private Double rmssd;
        private Double stressEma; //스트레스 지수
        private Double stressRaw;
    }
}

