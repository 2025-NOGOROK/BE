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

    /**
     * Google API에서 사용자 정보를 조회
     * @param accessToken 액세스 토큰
     * @return 사용자 정보 (email 포함)
     */
    public Map<String, Object> getGoogleUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v2/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        return response.getBody();
    }

    @Transactional
    public String getOrRefreshGoogleAccessToken(User user) {
        // 리프레시 토큰이 없으면 액세스 토큰 갱신이 불가능
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.warn("리프레시 토큰 없음. 구글 액세스 토큰만 반환.");
            return user.getGoogleAccessToken();
        }

        // 액세스 토큰 만료 여부 체크
        if (user.getGoogleAccessTokenExpiresAt() != null &&
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5).isBefore(user.getGoogleAccessTokenExpiresAt())) {
            log.debug("구글 액세스 토큰 유효: 만료되지 않았습니다.");
            return user.getGoogleAccessToken();  // 액세스 토큰이 유효하면 기존 토큰을 그대로 반환
        }

        log.info("구글 액세스 토큰 만료됨. 리프레시 토큰으로 갱신 시도...");
        String url = "https://oauth2.googleapis.com/token";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleOAuthProperties.getWebClientId());
        params.add("client_secret", googleOAuthProperties.getClientSecret());
        params.add("refresh_token", user.getGoogleRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("구글 액세스 토큰 갱신 실패");
            }

            Map<String, Object> body = response.getBody();
            String newAccessToken = (String) body.get("access_token");
            Long expiresIn = ((Number) body.get("expires_in")).longValue();
            LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn);

            // 갱신된 액세스 토큰 및 만료 시간을 DB에 저장
            user.setGoogleAccessToken(newAccessToken);
            user.setGoogleAccessTokenExpiresAt(expiresAt);
            userRepository.save(user);

            log.info("구글 액세스 토큰 갱신 성공: {}", newAccessToken);
            return newAccessToken;

        } catch (HttpClientErrorException e) {
            log.error("구글 액세스 토큰 갱신 오류: ", e);
            throw new RuntimeException("액세스 토큰 갱신 실패: " + e.getMessage(), e);
        }
    }


}
