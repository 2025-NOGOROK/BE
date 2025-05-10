package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.service.FcmService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {
    private final FcmService fcmService;
    public FcmController(FcmService fcmService) { this.fcmService = fcmService; }

    @PostMapping("/send")
    public String send(@RequestParam String token,
                       @RequestParam String title,
                       @RequestParam String body) throws Exception {
        return fcmService.sendMessage(token, title, body);
    }
}
