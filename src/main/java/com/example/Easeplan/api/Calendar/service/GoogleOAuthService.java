package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.config.GoogleOAuthProperties;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GoogleOAuthService {

    private final GoogleOAuthProperties googleOAuthProperties;
    private final UserRepository userRepository;

    public GoogleOAuthService(GoogleOAuthProperties googleOAuthProperties, UserRepository userRepository) {
        this.googleOAuthProperties = googleOAuthProperties;
        this.userRepository = userRepository;
    }

    /**
     * Google OAuth 2.0을 통해 authorization code로 액세스 토큰을 교환
     * @param code Authorization code
     * @return 액세스 토큰과 리프레시 토큰을 포함한 응답
     */
    public Map<String, Object> exchangeCodeForToken(String code) {
        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleOAuthProperties.getClientId());  // Android 클라이언트 ID
        params.add("redirect_uri", googleOAuthProperties.getRedirectUri());  // 모바일 앱의 딥링크 URI
        params.add("grant_type", "authorization_code");

        // Android 클라이언트에서는 client_secret이 필요 없으므로 포함하지 않음
        if (googleOAuthProperties.getClientSecret() != null && !googleOAuthProperties.getClientSecret().isEmpty()) {
            params.add("client_secret", googleOAuthProperties.getClientSecret());
        }

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

    /**
     * Google API에서 사용자 정보를 조회
     * @param accessToken 액세스 토큰
     * @return 사용자 정보 (email 포함)
     */
    public Map<String, Object> getGoogleUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);  // Bearer 방식으로 Authorization 헤더에 액세스 토큰 포함
        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody();
    }

    @Transactional
    public String getOrRefreshGoogleAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.warn("Android 기반 인증 사용자 - refresh_token 없음. 앱에서 access_token 갱신 필요.");
            return user.getGoogleAccessToken();
        }

        if (user.getGoogleAccessTokenExpiresAt() != null &&
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5).isBefore(user.getGoogleAccessTokenExpiresAt())) {
            log.debug("Google access token still valid for user: {}. No refresh needed.", user.getEmail());
            return user.getGoogleAccessToken();
        }

        log.info("Google access token expired or near expiration for user: {}. Attempting to refresh...", user.getEmail());

        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleOAuthProperties.getClientId());  // Android 클라이언트 ID
        params.add("client_secret", googleOAuthProperties.getClientSecret());  // 웹 클라이언트 ID
        params.add("refresh_token", user.getGoogleRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("액세스 토큰 갱신 실패 (HTTP {}): {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("액세스 토큰 갱신 실패: " + response.getBody());
            }

            Map<String, Object> body = response.getBody();
            String newAccessToken = (String) body.get("access_token");
            Long expiresInSeconds = ((Number) body.get("expires_in")).longValue();

            // expiresAt을 먼저 선언
            LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.now().plusSeconds(expiresInSeconds), ZoneOffset.UTC);

            // 그리고 나서 updateGoogleTokens에 전달
            user.updateGoogleTokens(newAccessToken, null, expiresAt);
            user.setGoogleAccessTokenExpiresAt(expiresAt);
            userRepository.save(user);

            log.info("Google access token refreshed successfully for user: {}", user.getEmail());
            return newAccessToken;

        } catch (HttpClientErrorException e) {
            log.error("Google access token refresh client error for user {}: HTTP {} - {}", user.getEmail(), e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Invalid refresh token for user {}. Clearing tokens to force re-authentication.", user.getEmail());
                user.updateGoogleTokens(null, null, null);
                user.setGoogleAccessTokenExpiresAt(null);
                userRepository.save(user);
                throw new RuntimeException("Refresh token is invalid. User needs to re-authenticate with Google.", e);
            }
            throw new RuntimeException("Failed to refresh Google access token: " + e.getMessage(), e);
        }
    }
}

