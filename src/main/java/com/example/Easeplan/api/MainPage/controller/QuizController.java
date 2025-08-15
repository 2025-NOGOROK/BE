package com.example.Easeplan.api.MainPage.controller;

import com.example.Easeplan.api.MainPage.dto.request.QuizSubmitRequest;
import com.example.Easeplan.api.MainPage.dto.response.QuizSubmitResponse;
import com.example.Easeplan.api.MainPage.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "메인페이지", description = "스트레스 퀴즈")
@RestController
@RequestMapping("/api/mainpage/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping
    @Operation(summary = "메인페이지: 스트레스 퀴즈", description = """
        스트레스 파악 퀴즈를 진행합니다. 점수에 따라 LOW, MODERATE,HIGH 결과를 반환합니다.<br>
        헤더에 accessToken을 넣어주세요.<br>
        """)
    public ResponseEntity<QuizSubmitResponse> submit(@AuthenticationPrincipal UserDetails userDetails,
                                                     @RequestBody @Valid QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submit(request));
    }
}
