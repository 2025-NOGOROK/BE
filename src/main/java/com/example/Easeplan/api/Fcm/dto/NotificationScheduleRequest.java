package com.example.Easeplan.api.Fcm.dto;

import java.time.ZonedDateTime;

public record NotificationScheduleRequest(
        String title,
        ZonedDateTime startDateTime,
        int minutesBeforeAlarm
) {}
