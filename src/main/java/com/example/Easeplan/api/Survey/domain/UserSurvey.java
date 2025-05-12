package com.example.Easeplan.api.Survey.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserSurvey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private User user; // 로그인된 유저(회원가입된 사람)의 식별자

    private String scheduleType; // "loose" or "tight"
    private Boolean suddenChangePreferred; // true/false
    private String chronotype; // "morning" or "evening"
    private String preferAlone; // "alone" or "together"
    private String stressReaction; // "lethargy", "anger", "overeating", "depression", "alone"
    private Boolean hasStressRelief; // true/false

    @ElementCollection
    private List<String> stressReliefMethods; // 최대 3개, hasStressRelief가 true일 때만
}
