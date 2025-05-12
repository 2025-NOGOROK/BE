package com.example.Easeplan.global.auth.controller;

import com.example.Easeplan.global.auth.dto.PasswordResetRequest;
import com.example.Easeplan.global.auth.dto.SignInRequest;
import com.example.Easeplan.global.auth.dto.SignUpRequest;
import com.example.Easeplan.global.auth.dto.TokenResponse;
import com.example.Easeplan.global.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        TokenResponse response = authService.signIn(request);
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponse<>("로그인에 성공했습니다.", response));
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
