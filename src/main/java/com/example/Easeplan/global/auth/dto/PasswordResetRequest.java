package com.example.Easeplan.global.auth.dto;

public record PasswordResetRequest(
        String email,
        String newPassword,
        String confirmPassword
) {}