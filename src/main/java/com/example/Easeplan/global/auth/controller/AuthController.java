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

    @Operation(summary = "회원가입", description = """ 
            회원가입을 진행합니다.""")
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
