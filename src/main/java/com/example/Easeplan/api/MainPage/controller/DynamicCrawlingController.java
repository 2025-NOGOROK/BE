package com.example.Easeplan.api.MainPage.controller;

import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService;
import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService1;
import com.example.Easeplan.api.MainPage.service.DynamicCrawlingService2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "메인페이지", description = "스트레스 관리API+여행추천API")
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




    @GetMapping("/api/crawl/samsung-stress")
    @Operation(summary = "메인페이지: 스트레스 관리 정보", description = """
        삼성병원에서 가져온 스트레스 관리 정보를 조회합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public String crawlSamsungStressPage(@AuthenticationPrincipal UserDetails userDetails) {
        // user가 null이면 인증 안 된 상태(자동으로 401 반환)
        return crawlingService.crawlSamsungHospital();
    }

    @GetMapping("/api/crawl/lawtimes-article")
    @Operation(summary = "메인페이지: 법률신문(스트레스관리) 기사 크롤링", description = """
        lawtimes.co.kr 기사에서 본문 글과 이미지를 크롤링합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public String crawlLawtimesArticle(@AuthenticationPrincipal UserDetails userDetails) {
        // user가 null이면 인증 안 된 상태(자동으로 401 반환)
        return crawlingService1.crawlLawtimesArticle();
    }

    @GetMapping("/api/crawl/trauma")
    @Operation(summary = "메인페이지: 국가트라우마(스트레스관리) 기사 크롤링", description = """
        국가트라우마 페이지에서 본문 글과 이미지를 크롤링합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public String crawlNctStressPage(@AuthenticationPrincipal UserDetails userDetails) {
        // user가 null이면 인증 안 된 상태(자동으로 401 반환)
        return crawlingService2.crawlNctStressPage();
    }



}
