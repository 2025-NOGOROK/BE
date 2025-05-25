package com.example.Easeplan.api.Recommend.Event.controller;

import com.example.Easeplan.api.Recommend.Event.service.CultureEventService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CultureEventController {
    private final CultureEventService cultureEventService;

    public CultureEventController(CultureEventService cultureEventService) {
        this.cultureEventService = cultureEventService;
    }
    @Operation(
            summary = "문화 이벤트 목록 조회",
            description = """
    분류명(dtype), 제목(title), 페이지 정보(numOfRows, pageNo)를 입력하여 문화 이벤트 목록을 조회합니다.
    
    - 분류명(dtype): 연극, 뮤지컬, 오페라, 음악, 콘서트, 국악, 무용, 전시, 기타 중 하나를 입력하세요.
    - 제목(title): 2자 이상 입력해야 정상적으로 동작합니다.
    - 액세스 토큰: Authorization 헤더에 Bearer {token} 형식으로 입력해야 합니다.
    """,
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "dtype",
                            description = "분류명 (연극, 뮤지컬, 오페라, 음악, 콘서트, 국악, 무용, 전시, 기타)",
                            required = true,
                            example = "연극"
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "title",
                            description = "이벤트 제목 (2자 이상 입력)",
                            required = true,
                            example = "햄릿"
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "numOfRows",
                            description = "한 페이지에 보여줄 이벤트 수 (기본값: 10)",
                            required = false,
                            example = "10"
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "pageNo",
                            description = "페이지 번호 (기본값: 1)",
                            required = false,
                            example = "1"
                    )
            }
    )
    @GetMapping("/api/culture/events")
    public String getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
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
