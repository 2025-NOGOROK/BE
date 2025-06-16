package com.example.Easeplan.global.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@Setter // Setter를 추가하여 필드에 값을 설정할 수 있도록 함
public class TokenResponse {
    private String accessToken;        // 자체 JWT
    private String refreshToken;       // 자체 Refresh Token
    private String googleAccessToken;  // 구글 액세스 토큰
    private String jwtToken;           // 구글 JWT
}
