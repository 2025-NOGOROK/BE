package com.example.Easeplan.api.Recommend.Tour.controller;

import com.example.Easeplan.api.Recommend.Tour.service.TourApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Tag(name = "메인페이지", description = "스트레스 관리API+여행추천API")
@RestController
public class TourApiController {

    private final TourApiService tourApiService;

    public TourApiController(TourApiService tourApiService) {
        this.tourApiService = tourApiService;
    }
    @Operation(summary = "위치기반 투어조회", description = """
           메인페이지: 위치기반(1km) 투어를 진행합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @GetMapping("/api/tour/location")
    public String getLocationBasedList(@AuthenticationPrincipal UserDetails userDetails,
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
