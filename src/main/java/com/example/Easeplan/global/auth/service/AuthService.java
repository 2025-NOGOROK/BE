package com.example.Easeplan.global.auth.service;

import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import com.example.Easeplan.global.auth.domain.RefreshToken;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.dto.*;
import com.example.Easeplan.global.auth.repository.RefreshTokenRepository;
import com.example.Easeplan.global.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final GoogleOAuthService oAuthService;

    // Constructor Injection for required dependencies
    public AuthService(JwtUtil jwtUtil, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       BCryptPasswordEncoder passwordEncoder, GoogleOAuthService oAuthService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.oAuthService = oAuthService;
    }

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 2중 확인
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // ★ 필수 약관 동의 검증 (여기에 추가)
        if (!request.termsOfServiceAgreed() || !request.privacyPolicyAgreed() ||
                !request.healthInfoPolicyAgreed() || !request.locationPolicyAgreed()) {
            throw new IllegalArgumentException("필수 약관에 모두 동의해야 합니다.");
        }

        // User 생성
        User newUser = User.builder()
                .name(request.name())
                .birth(request.birth())
                .gender(request.gender())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .pushNotificationAgreed(request.pushNotificationAgreed())
                .deviceToken(request.deviceToken())
                .termsOfServiceAgreed(request.termsOfServiceAgreed())
                .privacyPolicyAgreed(request.privacyPolicyAgreed())
                .healthInfoPolicyAgreed(request.healthInfoPolicyAgreed())
                .locationPolicyAgreed(request.locationPolicyAgreed())
                .build();

        User saved = userRepository.save(newUser);

        // 토큰 발급 등 기존 로직
        String accessToken = jwtUtil.createAccessToken(newUser);
        String refreshToken = jwtUtil.createRefreshToken(newUser);

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(saved)                     // 🔴 필수
                        .email(saved.getEmail())
                        .token(refreshToken)
                        .build()
        );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }



    @Transactional
    public TokenResponse signIn(SignInRequest request) {
        User user = userRepository.findByEmail(request.getEmail())  // 수정된 메서드 호출
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 1. 구글 액세스 토큰 갱신 (리프레시 토큰으로)
        String newGoogleAccessToken = oAuthService.getOrRefreshGoogleAccessToken(user);

        // 2. 리프레시 토큰과 함께 새로운 JWT 발급
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("토큰이 존재하지 않습니다."));
        String refreshToken = refreshTokenEntity.getToken();
        String accessToken = "";

        if (jwtUtil.isValidRefreshToken(refreshToken)) {
            accessToken = jwtUtil.createAccessToken(user);
        } else {
            refreshToken = jwtUtil.createRefreshToken(user);
            refreshTokenEntity.updateToken(refreshToken);
            accessToken = jwtUtil.createAccessToken(user);
        }

        // 구글 액세스 토큰을 포함한 응답 반환
        return TokenResponse.builder()
                .accessToken(accessToken)         // 자체 애플리케이션의 액세스 토큰
                .refreshToken(refreshToken)       // 애플리케이션의 리프레시 토큰
                .googleAccessToken(newGoogleAccessToken)  // 구글 액세스 토큰
                .build();
    }



    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }




    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원정보가 없습니다."));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    //로그아웃
    @Transactional
    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email); // 리프레시 토큰 삭제
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
