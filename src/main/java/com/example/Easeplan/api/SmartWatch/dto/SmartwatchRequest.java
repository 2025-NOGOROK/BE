package com.example.Easeplan.api.SmartWatch.dto;

import jakarta.validation.constraints.*;

public record SmartwatchRequest(
        @NotBlank String deviceId,
        @DecimalMin("0.0") @DecimalMax("100.0") Double stressIndex,
        @Min(30) @Max(250) Integer heartRate,
        @DecimalMin("30.0") @DecimalMax("45.0") Double temperature
) {}
