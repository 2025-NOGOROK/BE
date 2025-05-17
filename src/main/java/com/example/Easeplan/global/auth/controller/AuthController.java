package com.example.Easeplan.global.auth.controller;

import com.example.Easeplan.global.auth.dto.*;
import com.example.Easeplan.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;


@Tag(name = "Auth", description = "로그인관련 API")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 커스텀 응답 클래스
    public static class CustomResponse<T> {
        private String message;
        private T data;

        public CustomResponse(String message, T data) {
            this.message = message;
            this.data = data;
        }

        // Getter
        public String getMessage() { return message; }
        public T getData() { return data; }
    }

    @Operation(
            summary = "회원가입",
            description = """
        새로운 사용자를 등록합니다. 모든 필수 필드를 정확히 입력해야 하며 약관 동의는 모두 필수입니다.<br><br>
        
        <b>요청 본문 예시:</b>
        <pre>
{
  "name": "홍길동",
  "birth": "1990-01-01",
  "gender": "M",
  "password": "TestPassword123!",
  "email": "user@example.com",
  "confirmPassword": "TestPassword123!",
  "pushNotificationAgreed": true,
  "deviceToken": "fcm_token_1234",
  "termsOfServiceAgreed": true,
  "privacyPolicyAgreed": true,
  "healthInfoPolicyAgreed": true,
  "locationPolicyAgreed": true
}
        </pre>

        <b>필드 설명:</b>
        - name: 사용자 이름 (2~20자 한글/영문) <b>[필수]</b><br>
        - birth: 생년월일 (YYYY-MM-DD 형식) <b>[필수]</b><br>
        - gender: 성별 (M: 남성, F: 여성, null 허용) <b>[선택]</b><br>
        - password: 비밀번호 (8~20자, 영문+숫자+특수문자 조합) <b>[필수]</b><br>
        - email: 이메일 (유효한 이메일 형식) <b>[필수]</b><br>
        - confirmPassword: 비밀번호 확인 (password와 일치해야 함) <b>[필수]</b><br>
        - pushNotificationAgreed: 푸시 알림 동의 여부 <b>[필수]</b><br>
        - deviceToken: FCM 디바이스 토큰 (pushNotificationAgreed=true 시 필수)<br>
        - termsOfServiceAgreed: 이용약관 동의 <b>[필수]</b><br>
        - privacyPolicyAgreed: 개인정보 처리 방침 동의 <b>[필수]</b><br>
        - healthInfoAgreed: 건강정보 수집 동의 <b>[필수]</b><br>
        - locationPolicyAgreed: 위치기반 서비스 동의 <b>[필수]</b>

        <b>유효성 검사:</b>
        1. password와 confirmPassword는 반드시 일치해야 함
        2. 모든 약관 동의(terms~) 필드는 true여야 함
        3. pushNotificationAgreed=true인 경우 deviceToken 필수
        4. 이메일 중복 불가

        <b>응답:</b>
        - 201 Created: 회원가입 성공 (JWT 토큰 반환)
        - 400 Bad Request: 유효성 검사 실패
        - 409 Conflict: 이메일 중복
        """
    )
    @PostMapping("/signUp")
    public ResponseEntity<CustomResponse<TokenResponse>> signUp(@RequestBody SignUpRequest request) {
        authService.signUp(request); // 이메일이 DB에 저장됨
        try {
            TokenResponse response = authService.signUp(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new CustomResponse<>("회원가입 및 로그인에 성공했습니다.", response)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new CustomResponse<>(e.getMessage(), null)
            );
        }
    }
    @Operation(summary = "로그인", description = """
            로그인을 진행합니다.""")
    @PostMapping("/signIn")
    public ResponseEntity<CustomResponse<TokenResponse>> signIn(@RequestBody SignInRequest request) {
        try {
            TokenResponse response = authService.signIn(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new CustomResponse<>("로그인에 성공했습니다.", response));
        } catch (IllegalArgumentException e) {
            // 비밀번호 불일치, 존재하지 않는 사용자 등
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CustomResponse<>(e.getMessage(), null));
        }
    }
    @Operation(summary = "비밀번호 변경 시 이메일 확인", description = """
            비밀번호를 변경 시 이메일을 조회합니다.""")
    @PostMapping("/checkEmail")
    public ResponseEntity<?> checkEmail(@RequestBody EmailRequest request) {
        String email = request.getEmail();
        boolean exists = authService.existsByEmail(email);
        String message = exists ? "이메일이 존재합니다." : "일치하는 회원정보가 없습니다.";
        return ResponseEntity.ok(new CustomResponse<>(message, exists));
    }


    @Operation(summary = "비밀번호 변경", description = """
            비밀번호를 변경합니다.""")
    @PostMapping("/resetPassword")
    public ResponseEntity<CustomResponse<Void>> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(new CustomResponse<>("비밀번호가 성공적으로 변경되었습니다.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CustomResponse<>(e.getMessage(), null));
        }
    }
}
