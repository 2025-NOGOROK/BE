package com.example.Easeplan.api.Recommend.Long.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
 @NoArgsConstructor
@AllArgsConstructor
public class RecommendationOption {
    private String type;        // "calendar" or "event"
    private String label;       // 예: "전시/미술", "추천X" 등
    private Object data;        // FormattedTimeSlot, CalendarEvent 등
    private String startTime;   // ISO-8601, 통일!
    private String endTime;     // ISO-8601, 통일!
}

