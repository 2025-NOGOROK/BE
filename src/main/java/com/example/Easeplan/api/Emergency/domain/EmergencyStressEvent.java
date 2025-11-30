package com.example.Easeplan.api.Emergency.domain;

import com.example.Easeplan.global.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class EmergencyStressEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING → (사용자 확인 전), ACTIVATED → (활성화 버튼 클릭), EXPIRED → (만료/무시)

    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;

    public enum Status { PENDING, ACTIVATED, EXPIRED }

    public void markActivated() {
        this.status = Status.ACTIVATED;
        this.activatedAt = LocalDateTime.now();
    }

    public void expire() { this.status = Status.EXPIRED; }
}
