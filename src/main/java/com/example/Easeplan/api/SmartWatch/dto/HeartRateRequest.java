package com.example.Easeplan.api.SmartWatch.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeartRateRequest {
    private String email;
    private Float min;
    private Float max;
    private Float avg;
    private String startTime;
    private String endTime;
    private Integer count;
    private Float stress;
}

