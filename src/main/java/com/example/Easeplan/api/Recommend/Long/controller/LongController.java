package com.example.Easeplan.api.Recommend.Long.controller;

import com.example.Easeplan.api.Recommend.Long.RecommendationResult.RecommendationResult;
import com.example.Easeplan.api.Recommend.Long.dto.RecommendationOption;
import com.example.Easeplan.api.Recommend.Long.service.LongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "긴 추천", description = "공연예술 긴 추천 API")
@RestController
@RequestMapping("/api/long-recommend")
@RequiredArgsConstructor
public class LongController {


    private final LongService longService;
    @Operation(
            summary = "처음 캘린더+추천 일정 시나리오 조회",
            description = "오늘의 내 캘린더 일정과 추천 일정(오늘 행사하는 장르별 2개)을 합쳐서 반환합니다. "
                    + "각 추천 일정은 실제 캘린더에 삽입되기 전 미리보기 상태입니다."
    )
    @GetMapping
    public List<RecommendationOption> getLongRecommendations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        return longService.getLongRecommendations(email);
    }

    @Operation(
            summary = "전날 선택 기반 추천 일정 + 오늘 캘린더 시나리오 조회",
            description = "전날 사용자가 선택한 공연의 장르와 위치, 스트레스 정보를 기반으로 오늘의 추천 일정을 최대 2개 생성하여 "
                    + "오늘의 내 캘린더 일정과 함께 시나리오 형태로 반환합니다. "
                    + "추천 일정은 실제 캘린더에 삽입되기 전 미리보기 상태이며, "
                    + "위도(latitude)와 경도(longitude)는 사용자 현재 위치 기반 추천을 위해 필수로 전달됩니다."
    )
    @GetMapping("/tomorrow")
    public List<RecommendationOption> getTomorrowRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = true) Double latitude,
            @RequestParam(required = true) Double longitude
    ) {
        String email = userDetails.getUsername();
        return longService.recommendForTomorrow(email, latitude, longitude);
    }




    @Operation(
            summary = "사용자 선택 시나리오 저장",
            description = "3가지 시나리오(캘린더만, 추천1, 추천2) 중 사용자가 선택한 1개를 저장합니다. "
                    + "추천 일정(type=event) 선택 시 구글 캘린더에 실제로 삽입됩니다."
    )
    @PostMapping("/choice")
    public ResponseEntity<String> saveUserChoice(
            @RequestBody RecommendationOption choice,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        try {
            longService.saveUserChoice(email, choice);
            return ResponseEntity.ok("저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}
