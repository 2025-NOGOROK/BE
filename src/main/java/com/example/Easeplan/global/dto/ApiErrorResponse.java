package com.example.Easeplan.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
    private String error;
    private String message;
    private String authUrl; // 일반 에러면 null
}
