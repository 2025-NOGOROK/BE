package com.example.Easeplan.global.auth.dto;

import lombok.Builder;

@Builder
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String message
) {

}
