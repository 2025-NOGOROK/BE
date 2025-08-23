package com.example.Easeplan.api.Survey.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserSurvey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String scheduleType;             // "loose" / "tight"
    private Boolean suddenChangePreferred;   // true/false
    private String chronotype;               // "morning" / "evening"
    private String preferAlone;              // "alone" / "together"
    private String stressReaction;           // "lethargy", "anger", "overeating", "depression", "alone"
    private Boolean hasStressRelief;         // true/false

    @ElementCollection
    @Builder.Default
    private List<String> stressReliefMethods = new ArrayList<>(); // 최대 3개

    /** 도메인 업데이트 메서드 (세터 대체) */
    public void updateSurvey(
            String scheduleType,
            Boolean suddenChangePreferred,
            String chronotype,
            String preferAlone,
            String stressReaction,
            Boolean hasStressRelief,
            List<String> methods
    ) {
        this.scheduleType = scheduleType;
        this.suddenChangePreferred = suddenChangePreferred;
        this.chronotype = chronotype;
        this.preferAlone = preferAlone;
        this.stressReaction = stressReaction;
        this.hasStressRelief = hasStressRelief;

        // collection은 교체보다 "내용 갱신"이 안전
        this.stressReliefMethods.clear();
        if (methods != null) this.stressReliefMethods.addAll(methods);
    }
}
