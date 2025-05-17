package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
@Tag(name = "FCM", description = "FCM API")
@RestController
@RequestMapping("/api/fcm")
public class FcmController {
    private final FcmService fcmService;
    public FcmController(FcmService fcmService) { this.fcmService = fcmService; }
    @Operation(summary = "하루기록 FCM", description = """
            특정 시간에 알림을 보냅니다.""")
    @PostMapping("/send")
    public String send(@RequestParam String token,
                       @RequestParam String title,
                       @RequestParam String body) throws Exception {
        return fcmService.sendMessage(token, title, body);
    }
}
