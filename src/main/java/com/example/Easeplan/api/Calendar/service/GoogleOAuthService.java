package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.config.GoogleOAuthProperties;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.exception.GlobalExceptionHandler;
import com.example.Easeplan.global.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Service
public class GoogleOAuthService {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public GoogleOAuthService(GoogleOAuthProperties googleOAuthProperties, UserRepository userRepository) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.userRepository = userRepository;
    }

    // Google OAuth 2.0을 통해 authorization code로 액세스 토큰을 교환
    public Map<String, Object> exchangeCodeForToken(String code) {
        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleOAuthProperties.getWebClientId());
        params.add("client_secret", googleOAuthProperties.getClientSecret());
        params.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Token exchange failed: HTTP {} - {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Token exchange failed: " + response.getBody());
        }
        return response.getBody();
    }

    // Google API에서 사용자 정보를 조회
    // Google API에서 사용자 정보를 조회
    public Map<String, Object> getGoogleUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); // Bearer로 설정
        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("구글 사용자 정보 가져오기 실패", e);
            throw new RuntimeException("구글 사용자 정보 가져오기 실패");
        }
    }


    // JWT에서 이메일을 추출하는 메서드

    // JWT 토큰에서 이메일을 추출하는 메서드
    public String getGoogleUserEmailFromJwt(String jwtToken) {
        try {
            // 네가 서버에서 서명한 secret key
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)  // 또는 jwtUtil.getSecretKey()
                    .parseClaimsJws(jwtToken)
                    .getBody();

            return claims.get("email", String.class); // 또는 "sub", "username" 등
        } catch (Exception e) {
            log.error("JWT 파싱 실패", e);
            throw new RuntimeException("유효하지 않은 자체 JWT 토큰입니다.");
        }
    }


    // 구글 액세스 토큰을 리프레시 토큰으로 갱신
    @Transactional
    public String getOrRefreshGoogleAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isBlank()) {
            throw new GlobalExceptionHandler.GoogleRelinkRequiredException(
                    "No refresh token stored; relink required");
        }

        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleOAuthProperties.getWebClientId());
        params.add("client_secret", googleOAuthProperties.getClientSecret());
        params.add("refresh_token", user.getGoogleRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> r = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(params, headers), Map.class);

            Map<String, Object> body = r.getBody();
            String newAccess = (String) body.get("access_token");
            Long expiresIn = ((Number) body.get("expires_in")).longValue();

            user.setGoogleAccessToken(newAccess);
            user.setGoogleAccessTokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn));
            userRepository.save(user);
            return newAccess;

        } catch (HttpClientErrorException e) {
            String resp = e.getResponseBodyAsString();
            if (e.getStatusCode().is4xxClientError() && resp != null && resp.contains("invalid_grant")) {
                user.unlinkGoogle();
                userRepository.save(user);
                throw new GlobalExceptionHandler.GoogleRelinkRequiredException(
                        "Google tokens revoked/expired; relink required"
                );
            }
            throw new RuntimeException("액세스 토큰 갱신 실패: " + e.getMessage(), e);
        }


    }

    public String buildAuthUrl() {
        String scope = String.join(" ",
                "openid", "email", "profile",
                "https://www.googleapis.com/auth/calendar",
                "https://www.googleapis.com/auth/calendar.events"
        );
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleOAuthProperties.getWebClientId()
                + "&redirect_uri=" + URLEncoder.encode(googleOAuthProperties.getRedirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true";
    }
}



