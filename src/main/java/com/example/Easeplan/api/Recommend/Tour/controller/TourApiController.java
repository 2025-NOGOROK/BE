package com.example.Easeplan.api.Recommend.Tour.controller;

import com.example.Easeplan.api.Recommend.Tour.service.TourApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TourApiController {

    private final TourApiService tourApiService;

    public TourApiController(TourApiService tourApiService) {
        this.tourApiService = tourApiService;
    }

    @GetMapping("/api/tour/location")
    public String getLocationBasedList(
            @RequestParam(required=false,defaultValue = "10") int numOfRows,
            @RequestParam(required=false,defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "ETC") String mobileOS,
            @RequestParam(defaultValue = "AppTest") String mobileApp,
            @RequestParam double mapX,
            @RequestParam double mapY,
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(defaultValue = "json") String type
    ) throws Exception {
        return tourApiService.getLocationBasedList(
                numOfRows, pageNo, mobileOS, mobileApp, mapX, mapY, radius, type
        );
    }
}
