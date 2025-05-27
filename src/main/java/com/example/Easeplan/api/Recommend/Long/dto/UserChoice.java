package com.example.Easeplan.api.Recommend.Long.dto;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
public class UserChoice {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String type;      // "calendar" or "event"
    private String label;     // 장르명 등
    private String startTime; // ISO-8601
    private String endTime;   // ISO-8601

    // (필요시) 추천 일정의 title, description 등 추가
    private String eventTitle;
    private String eventDescription;
    // ...
}
