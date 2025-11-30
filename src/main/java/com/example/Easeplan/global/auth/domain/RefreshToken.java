package com.example.Easeplan.global.auth.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder // ✅ 클래스 레벨 빌더: user/email/token 모두 빌더에 포함
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB: refresh_token.user_id (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 512)
    private String token;

    public void updateToken(String token) { this.token = token; }
}
