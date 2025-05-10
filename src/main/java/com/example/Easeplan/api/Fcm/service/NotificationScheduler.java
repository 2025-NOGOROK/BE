package com.example.Easeplan.api.Fcm.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class NotificationScheduler {
    private final TaskScheduler taskScheduler;
    private final FcmService fcmService;

    public NotificationScheduler(TaskScheduler taskScheduler, FcmService fcmService) {
        this.taskScheduler = taskScheduler;
        this.fcmService = fcmService;
    }

    public void scheduleAlarm(String token, String title, String body, Instant minutesBeforeAlarm) {
        taskScheduler.schedule(() -> {
            try {
                fcmService.sendMessage(token, title, body);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, Date.from(minutesBeforeAlarm));
    }
}
