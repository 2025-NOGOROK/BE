package com.example.Easeplan.api.Recommend.Event.controller;

import com.example.Easeplan.api.Recommend.Event.service.CultureEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CultureEventController {
    private final CultureEventService cultureEventService;

    public CultureEventController(CultureEventService cultureEventService) {
        this.cultureEventService = cultureEventService;
    }

    @GetMapping("/api/culture/events")
    public String getEvents(
            @RequestParam String dtype,
            @RequestParam String title,
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo
    ) throws Exception {

        // title은 2자 이상이어야 정상 동작
        if (title == null || title.trim().length() < 2) {
            throw new IllegalArgumentException("title은 2자 이상이어야 합니다.");
        }
        return cultureEventService.getEvents(dtype, title, numOfRows, pageNo);
    }
}
