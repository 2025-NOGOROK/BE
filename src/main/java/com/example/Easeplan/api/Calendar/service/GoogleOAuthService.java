package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 임포트

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GoogleOAuthService {

    @Value("${google.client-id}")
    private String clientId;

//    @Value("${google.client-secret}")
//    private String clientSecret;

//    @Value("${google.redirect-uri}")
//    private String redirectUri;

    // yml에서 스코프 목록을 주입받아 사용 (실제 인증 요청 시 활용)
//    @Value("${google.scope}")
//    private List<String> scopes;

    private final UserRepository userRepository;

    public GoogleOAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 인증 코드를 구글 토큰으로 교환합니다. (최초 로그인/연동 시 사용)
     * @param code 구글로부터 받은 인증 코드
     * @return 액세스 토큰, 리프레시 토큰, 만료 시간 등이 포함된 맵
     */
    public Map<String, Object> exchangeCodeForToken(String code) {
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
      //  params.add("client_secret", clientSecret);
      //  params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        // params.add("scope", String.join(" ", scopes)); // 만약 구글 인증 흐름을 직접 제어한다면 이 부분을 추가해야 합니다.
        // 그러나 보통 Spring Security OAuth2 클라이언트가 이를 처리합니다.

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("토큰 교환 실패: HTTP {} - {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("토큰 교환 실패: " + response.getBody());
        }
        return response.getBody();
    }

    /**
     * 액세스 토큰으로 구글 사용자 정보를 조회합니다.
     * @param accessToken 구글 액세스 토큰
     * @return 사용자 이메일, 이름 등이 포함된 맵
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

    /**
     * 사용자에게 유효한 구글 액세스 토큰을 반환합니다.
     * 토큰이 만료되었거나 만료 임박 시 refresh_token을 사용하여 갱신을 시도하고 DB에 저장합니다.
     * @param user 토큰 정보를 가진 사용자 엔티티
     * @return 유효한 액세스 토큰
     * @throws RuntimeException 토큰 갱신 실패 또는 refresh token 없음 시 발생
     */
    @Transactional // DB 업데이트가 있으므로 트랜잭션으로 묶습니다.
    public String getOrRefreshGoogleAccessToken(User user) {
        // refresh token이 없으면 바로 예외 발생 (재인증 필요)
// 대체 방식
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.warn("Android 기반 인증 사용자 - refresh_token 없음. 앱에서 access_token 갱신 필요.");
            return user.getGoogleAccessToken(); // 그대로 사용 (유효한 경우만)
        }



        // Access Token이 아직 유효하고, 만료까지 5분 이상 남았으면 갱신하지 않고 바로 반환
        // (네트워크 요청 최소화를 위함)
        if (user.getGoogleAccessTokenExpiresAt() != null &&
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5).isBefore(user.getGoogleAccessTokenExpiresAt())) {
            log.debug("Google access token still valid for user: {}. No refresh needed.", user.getEmail());
            return user.getGoogleAccessToken();
        }

        log.info("Google access token expired or near expiration for user: {}. Attempting to refresh...", user.getEmail());

        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
       // params.add("client_secret", clientSecret);
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

            // **[핵심 변경]** 갱신된 토큰 정보와 만료 시각을 User 엔티티에 업데이트하고 DB에 저장
            // refresh token은 갱신 시 보통 변하지 않으므로 null을 전달 (User::updateGoogleTokens에서 처리)
            user.updateGoogleTokens(newAccessToken, null);
            // 현재 시각에 expiresInSeconds를 더하여 만료 시각을 계산하고 UTC 기준으로 저장
            user.setGoogleAccessTokenExpiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(expiresInSeconds), ZoneOffset.UTC));
            userRepository.save(user); // **갱신된 토큰 정보를 DB에 저장**

            log.info("Google access token refreshed successfully for user: {}", user.getEmail());
            return newAccessToken;

        } catch (HttpClientErrorException e) {
            log.error("Google access token refresh client error for user {}: HTTP {} - {}", user.getEmail(), e.getStatusCode(), e.getResponseBodyAsString());
            // 400 Bad Request (invalid_grant 등) 에러는 refresh token이 유효하지 않음을 의미
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Invalid refresh token for user {}. Clearing tokens to force re-authentication.", user.getEmail());
                user.updateGoogleTokens(null, null); // 유효하지 않은 토큰 제거
                user.setGoogleAccessTokenExpiresAt(null); // 만료 시각도 제거
                userRepository.save(user); // DB에 변경사항 반영
                throw new RuntimeException("Refresh token is invalid. User needs to re-authenticate with Google.", e);
            }
            throw new RuntimeException("Failed to refresh Google access token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unknown error during Google access token refresh for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to refresh Google access token: " + e.getMessage(), e);
        }
    }



    public class ReAuthenticationRequiredException extends RuntimeException {

        public ReAuthenticationRequiredException() {
            super("Google 인증이 필요합니다. 다시 로그인하세요.");
        }

        public ReAuthenticationRequiredException(String message) {
            super(message);
        }

        public ReAuthenticationRequiredException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}