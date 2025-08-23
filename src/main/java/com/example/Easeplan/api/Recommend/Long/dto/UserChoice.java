package com.example.Easeplan.api.Recommend.Long.dto;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
@AllArgsConstructor
@Builder
public class UserChoice {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String type;       // "calendar" or "event"
    private String label;      // 장르명 등
    private String startTime;  // ISO-8601
    private String endTime;    // ISO-8601

    private String eventTitle;
    private String eventDescription;
}
