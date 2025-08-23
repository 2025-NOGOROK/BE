package com.example.Easeplan.api.Recommend.Event.controller;

import com.example.Easeplan.api.Recommend.Event.service.CultureEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@SecurityRequirement(name = "bearerAuth")
@RestController
public class CultureEventController {
    private final CultureEventService cultureEventService;

    public CultureEventController(CultureEventService cultureEventService) {
        this.cultureEventService = cultureEventService;
    }


    @GetMapping("/api/culture/events")
    public String getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String codename,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "10") int endIndex
    ) throws Exception {
        // date가 없으면 오늘 날짜로 자동 세팅
        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return cultureEventService.getEvents(codename, title, date, startIndex, endIndex);
    }



}
