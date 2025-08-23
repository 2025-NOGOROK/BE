package com.example.Easeplan.global.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true) // ← 기존 객체에서 일부 필드만 바꿔 새로 만들 때 사용
public class TokenResponse {
    private final String accessToken;        // 자체 JWT
    private final String refreshToken;       // 자체 Refresh Token
    private final String googleAccessToken;  // 구글 액세스 토큰
    private final String jwtToken;           // 구글 JWT
}
