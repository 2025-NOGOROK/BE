package com.example.Easeplan.api.Recommend.Short.controller;

import com.example.Easeplan.api.Calendar.dto.FormattedTimeSlot;
import com.example.Easeplan.api.Recommend.Short.service.ShortService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "짧은 추천", description = "짧은 쉼표")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class ShortController {

    private final UserRepository userRepository;
    private final ShortService shortService;

    @Autowired
    public ShortController(UserRepository userRepository,
                           ShortService shortService) {
        this.userRepository = userRepository;
        this.shortService = shortService;
    }


    @Operation(
            summary = "짧은 추천 2개 생성 및 캘린더 반영",
            description = """
            설문 기반 추천 활동 목록에서 조합(쌍)을 순환하며, 호출 1회당 2개의 짧은 활동을 선택해
            해당 날짜의 사용자의 Google Calendar에 이벤트로 추가합니다.
            - 추천은 설문 조합/날짜 기준으로 중복 없이 회전합니다.
            - 빈 슬롯이 부족하면 10:00부터 fallback 슬롯을 생성합니다.
            - 시간 형식은 KST(Asia/Seoul) 기준으로 저장됩니다.
            """)
    @PostMapping("/api/short-recommend")
    public ResponseEntity<List<FormattedTimeSlot>> createAndSaveShortRecommendation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String date
    ) {
        try {
            if (userDetails == null) return ResponseEntity.status(401).build();

            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없음"));

            LocalDate targetDate = LocalDate.parse(date);
            List<FormattedTimeSlot> result = shortService.generateTwoShortBreaks(user, targetDate);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
