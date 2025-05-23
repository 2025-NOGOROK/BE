package com.example.Easeplan.api.Mypage.controller;

import com.example.Easeplan.api.Calendar.service.GoogleCalendarService;
import com.example.Easeplan.api.Mypage.service.MyPageService;
import com.example.Easeplan.api.SmartWatch.dto.HeartRateRequest;
import com.example.Easeplan.api.SmartWatch.service.SmartwatchService;
import com.example.Easeplan.api.Survey.dto.UserSurveyRequest;
import com.example.Easeplan.api.Survey.service.UserSurveyService;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Operation(
            summary = "생활패턴 설문 수정",
            description = """
            사용자의 생활패턴 설문 데이터를 업데이트합니다.<br>
            <b>헤더에 accessToken을 포함해야 합니다.</b><br><br>
            
            <b>요청 본문 예시:</b>
            <pre>
{
  "scheduleType": "타이트",
  "suddenChangePreferred": true,
  "chronotype": "저녁",
  "preferAlone": "혼자",
  "stressReaction": "감각 회피형",
  "hasStressRelief": true,
  "stressReliefMethods": ["명상", "요가"]
}
            </pre>
            
            <b>응답:</b>
            - 200 OK: 수정 성공
            - 400 Bad Request: 유효하지 않은 데이터
            - 401 Unauthorized: 인증 실패
            """
    )
    @PutMapping("/survey")
    public void updateSurvey(
            @AuthenticationPrincipal User user,
            @RequestBody UserSurveyRequest request

    ) {
        surveyService.saveSurvey(user, request);
    }


//    @Operation(summary = "심박수 데이터 수정",description = """
//        ### 저장 예시
//        ```
//        {
//          "email": "user@example.com",
//          "min": 60.0,
//          "max": 120.0,
//          "avg": 80.0,
//          "startTime": "2025-05-18T14:00:00",
//          "endTime": "2025-05-18T14:30:00",
//          "count": 150,
//          "stress": 75.5
//        }
//        ```
//        """)
//    @PutMapping("/heartrate")
//    public ResponseEntity<?> updateData(@RequestBody @Valid HeartRateRequest request) {
//        try {
//            smartwatchService.updateDeviceData(request);
//            return ResponseEntity.ok("데이터 수정 성공");
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

    // 로그아웃
//    @Operation(summary = "로그아웃", description = """
//            앱 로그아웃을 진행합니다.<br>
//            헤더에 accessToken을 넣어주세요.<br>
//            """)
//    @PostMapping("/logout")
//    public ResponseEntity<String> logout(@AuthenticationPrincipal User user, HttpSession session
//                                         ) {
//        myPageService.logout(user.getEmail());
//        if (session != null) session.invalidate();
//        return ResponseEntity.ok("로그아웃 완료");
//    }

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
