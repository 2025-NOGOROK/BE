package com.example.Easeplan.api.Mypage.controller;

import com.example.Easeplan.api.Calendar.dto.CalendarEventRequest;
import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Mypage.service.MyPageService;
import com.example.Easeplan.api.SmartWatch.domain.SmartwatchData;
import com.example.Easeplan.api.SmartWatch.dto.SmartwatchRequest;
import com.example.Easeplan.api.SmartWatch.service.SmartwatchService;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@Tag(name = "마이페이지", description = "마이페이지 API")
@SecurityRequirement(name = "accessToken")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
public class MyPageController {

    private final UserSurveyService surveyService;
    private final GoogleCalendarService calendarService;
    private final SmartwatchService smartwatchService;
    private final AuthService authService;
    private final MyPageService myPageService; // 추가
    // 설문 정보 수정

    @Operation(summary = "생활패턴 수정", description = """
            생활패턴을 수정합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @PutMapping("/survey")
    public void updateSurvey(
            @AuthenticationPrincipal User user,
            @RequestBody UserSurveyRequest request

    ) {
        surveyService.saveSurvey(user, request);
    }


    // 스마트워치 연동
    @Operation(summary = "스마트 워치 변경", description = """
            스마트 워치내용을 변경합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @PostMapping("/smartwatch")
    public void connectSmartwatch(
            @AuthenticationPrincipal User user,
            @RequestBody SmartwatchRequest request
    ) {
        smartwatchService.connectDevice(user, request);
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = """
            앱 로그아웃을 진행합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal User user, HttpSession session
                                         ) {
        myPageService.logout(user.getEmail());
        if (session != null) session.invalidate();
        return ResponseEntity.ok("로그아웃 완료");
    }

    // 회원 탈퇴
    @Operation(summary = "회원탈퇴", description = """
            회원탈퇴를 진행합니다.<br>
            헤더에 accessToken을 넣어주세요.<br>
            """)
    @DeleteMapping("/user/delete")
    public ResponseEntity<String> deleteUser(@AuthenticationPrincipal User user, HttpSession session
                                            ) {
        myPageService.deleteUser(user.getEmail());
        if (session != null) session.invalidate();
        return ResponseEntity.ok("탈퇴 완료");
    }
}
