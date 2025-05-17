package com.example.Easeplan.api.Fcm.controller;

import com.example.Easeplan.api.Fcm.service.FcmService;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM", description = "FCM API")
@RestController
@RequestMapping("/api/fcm")
public class FcmController {
    private final FcmService fcmService;
    private final UserRepository userRepository; // ✅ 추가

    // ✅ 생성자에 UserRepository 주입
    public FcmController(FcmService fcmService, UserRepository userRepository) {
        this.fcmService = fcmService;
        this.userRepository = userRepository;
    }
    @Operation(summary = "일정 FCM", description = """
            FCM 토큰을 서버에 저장합니다.<br>
            헤더에 accessToken과 쿼리 파라미터로 FCM 코드를 넣어주세요.<br>
            """)
    @PostMapping("/register")
    public ResponseEntity<Void> registerFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String token
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.addFcmToken(token);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }
    @Operation(summary = "하루기록 FCM", description = """
        입력한 FCM 토큰으로 즉시 푸시 알림을 보냅니다.
        파라미터로 FCM 토큰, 알림 제목(title), 알림 내용(body)을 전달하세요.<br><br>
        예시: <br>
        <code>POST /api/fcm/send?token=FCM_토큰&title=제목&body=내용</code>
        """)
    @PostMapping("/send")
    public String send(@RequestParam String token,
                       @RequestParam String title,
                       @RequestParam String body) throws Exception {
        return fcmService.sendMessage(token, title, body);
    }



}
