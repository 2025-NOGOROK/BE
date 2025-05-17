package com.example.Easeplan.api.SmartWatch.dto;

import jakarta.validation.constraints.*;

public record SmartwatchRequest(
        String deviceId,
        Double stressIndex, // ✅
        Integer heartRate,  // ✅
        Double temperature
) {}
