package com.example.Easeplan.api.Recommend.Long.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationOption {
    private String type;       // "calendar" or "event"
    private String label;      // 예: "전시/미술", "추천X" 등
    private Object data;       // FormattedTimeSlot 또는 리스트 등
    private String startTime;  // ISO-8601
    private String endTime;    // ISO-8601

    @JsonCreator
    public RecommendationOption(
            @JsonProperty("type") String type,
            @JsonProperty("label") String label,
            @JsonProperty("data") Object data,
            @JsonProperty("startTime") String startTime,
            @JsonProperty("endTime") String endTime
    ) {
        this.type = type;
        this.label = label;
        this.data = data;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
