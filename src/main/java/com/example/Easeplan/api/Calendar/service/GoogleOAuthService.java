package com.example.Easeplan.api.Calendar.service;

import com.example.Easeplan.api.Calendar.config.GoogleOAuthProperties;
import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
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

    // JWT 토큰 검증 메서드
    public boolean isValidGoogleJwtToken(String accessToken) {
        try {
            // GoogleIdTokenVerifier 객체 생성
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // GoogleIdTokenVerifier 설정
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleOAuthProperties.getWebClientId()))  // 클라이언트 ID로 검증
                    .build();

            // JWT 토큰 검증
            GoogleIdToken idToken = verifier.verify(accessToken);
            if (idToken != null) {
                log.info("Google JWT Token is valid.");
                return true;
            } else {
                log.warn("Invalid Google JWT token.");
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying Google JWT token", e);
            return false;
        }
    }

    // Google JWT에서 이메일을 추출
    public String getEmailFromGoogleJwtToken(String token) {
        try {
            // GoogleIdTokenVerifier 객체 생성
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // GoogleIdTokenVerifier 설정
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(Collections.singletonList(googleOAuthProperties.getWebClientId()))  // 클라이언트 ID로 검증
                    .build();

            // GoogleIdToken을 사용하여 토큰을 검증
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                return idToken.getPayload().getEmail(); // 이메일 추출
            } else {
                throw new RuntimeException("Invalid Google JWT token.");
            }
        } catch (Exception e) {
            log.error("Error extracting email from Google JWT token", e);
            throw new RuntimeException("Error extracting email from Google JWT token", e);
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
        if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
            log.warn("리프레시 토큰 없음. 구글 액세스 토큰만 반환.");
            return user.getGoogleAccessToken(); // 리프레시 토큰이 없으면 기존 액세스 토큰을 반환
        }

        // 리프레시 토큰을 사용하여 새로운 액세스 토큰을 요청
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

            Map<String, Object> responseBody = response.getBody();
            String newAccessToken = (String) responseBody.get("access_token");
            Long expiresIn = ((Number) responseBody.get("expires_in")).longValue();
            LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresIn);

            // 새로운 액세스 토큰 및 만료 시간 저장
            user.setGoogleAccessToken(newAccessToken);
            user.setGoogleAccessTokenExpiresAt(expiresAt);
            userRepository.save(user); // 유저 정보 업데이트

            log.info("구글 액세스 토큰 갱신 성공");
            return newAccessToken;

        } catch (HttpClientErrorException e) {
            log.error("구글 액세스 토큰 갱신 오류: ", e);
            throw new RuntimeException("액세스 토큰 갱신 실패: " + e.getMessage(), e);
        }
    }


}
