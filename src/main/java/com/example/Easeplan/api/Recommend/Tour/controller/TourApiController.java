package com.example.Easeplan.api.Recommend.Tour.controller;

import com.example.Easeplan.api.Recommend.Tour.dto.TourApiResponse;
import com.example.Easeplan.api.Recommend.Tour.service.TourApiService;
import com.example.Easeplan.api.Recommend.Tour.service.TourDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메인페이지", description = "스트레스 관리API+여행추천API")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class TourApiController {

    private final TourApiService tourApiService;
    private final TourDetailService tourDetailService;


    public TourApiController(TourApiService tourApiService, TourDetailService tourDetailService) {
        this.tourApiService = tourApiService;
        this.tourDetailService = tourDetailService;
    }





    @Operation(summary = "위치기반 투어조회", description = """
           메인페이지: 중심 좌표 기준 반경 약 10km 내 투어 정보를 제공합니다.<br>
           헤더에 accessToken을 넣어주세요.<br>
           """)
    @GetMapping("/api/tour/location")
    public String getLocationBasedList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "5") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam double mapX,
            @RequestParam double mapY
    ) throws Exception {
        return tourApiService.getLocationBasedList(numOfRows, pageNo, mapX, mapY);
    }


    @Operation(
            summary = "투어 상세 조회",
            description = """
        추천된 문화 행사의 상세 정보를 조회합니다.<br>
        예: seq=305418<br>
        - seq: 추천된 행사 목록 응답 중 하나의 seq 값<br>
        - 현 위치와의 거리 차이는 `distanceText` 필드에 km 단위로 표시됩니다.
        """
    )

    @GetMapping("/api/tour/detail")
    public String getTourDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String seq,
            @RequestParam(required = false) Double mapX, // 현재 경도
            @RequestParam(required = false) Double mapY  // 현재 위도
    ) throws Exception {
        return tourDetailService.getDetailInfo(seq, mapX, mapY);
    }



}
