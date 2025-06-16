package com.example.Easeplan.global.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String googleAccessToken;  // Add this field if it is part of the response
}
