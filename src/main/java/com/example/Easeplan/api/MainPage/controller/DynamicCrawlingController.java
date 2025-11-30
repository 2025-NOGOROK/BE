package com.example.Easeplan.api.MainPage.controller;

import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService;
import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService1;
import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@Tag(name = "메인페이지", description = "스트레스 관리API+여행추천API")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class DynamicCrawlingController {
    private final DynamicCrawlingService1 crawlingService1;

    private final DynamicCrawlingService crawlingService;
    private final DynamicCrawlingService2 crawlingService2;

    public DynamicCrawlingController(DynamicCrawlingService crawlingService,
                                     DynamicCrawlingService1 crawlingService1,
                                     DynamicCrawlingService2 crawlingService2) {
        this.crawlingService = crawlingService;
        this.crawlingService1 = crawlingService1;
        this.crawlingService2 = crawlingService2;
    }




    @GetMapping("/api/crawl/doctornow")
    @Operation(summary = "메인페이지: 스트레스 관리 정보", description = """
        닥터나우에서 가져온 스트레스 관리 정보를 조회합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public Map<String, Object> crawlSamsungStressPage(@AuthenticationPrincipal UserDetails userDetails) {
        return crawlingService.crawlStressArticleWithImages();
    }

    @GetMapping("/api/crawl/teen")
    @Operation(summary = "메인페이지: 10대 스트레스 기사 크롤링", description = """
        teen03 페이지에서 본문 글과 이미지를 크롤링합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public List<Map<String, String>> getTeenStressExercises(@AuthenticationPrincipal UserDetails userDetails) {
        return crawlingService1.crawlTeenStressSectionWithImage();
    }

    @GetMapping("/api/crawl/sciencetimes")
    @Operation(summary = "메인페이지: sciencetimes 기사 크롤링", description = """
        sciencetimes 페이지에서 본문 글과 이미지를 크롤링합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public List<Map<String, String>> crawlBoardmixArticle(@AuthenticationPrincipal UserDetails userDetails) {
        // user가 null이면 인증 안 된 상태(자동으로 401 반환)
        return crawlingService2.crawlBoardmixArticle();
    }



}
