package com.example.Easeplan.global.auth.dto;

import com.example.Easeplan.global.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@Getter
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(User user) {
        return Jwts.builder()
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(User user) {
        return Jwts.builder()
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean isValidAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                log.debug("토큰 만료일자: " + expiration);
                return expiration != null && !expiration.before(new Date());
            }
            log.error("토큰의 클레임이 null입니다.");
            return false;
        } catch (Exception e) {
            log.error("토큰 유효성 검사 중 예외 발생: ", e);
            return false;
        }
    }


    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                log.debug("리프레시 토큰 만료일자: " + expiration); // 만료일자 로깅
                return expiration != null && !expiration.before(new Date());
            }
            log.error("리프레시 토큰의 클레임이 null입니다."); // 클레임이 null인 경우 로깅
            return false;
        } catch (Exception e) {
            log.error("리프레시 토큰 유효성 검사 중 예외 발생: ", e); // 예외 발생 시 로깅
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("토큰 파싱 실패: ", e); // 파싱 실패 시 로깅
            throw new RuntimeException("토큰 파싱 실패");
        }
    }


    // Bearer 접두사 제거
    public String cleanBearer(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : header;
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    public String createToken(String email) {
        return Jwts.builder()
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    // 구글 OAuth access_token 유효성 검사
    // Google Access Token 정보 검증 (tokeninfo 사용)
    public boolean isValidGoogleAccessToken(String accessToken) {
        try {
            // Google OAuth Tokeninfo API를 사용하여 토큰 정보 검증
            String url = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + accessToken;
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // 응답이 정상이라면 유효한 토큰으로 판단
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Google Access Token is valid.");
                return true;
            } else {
                log.warn("Google Access Token is invalid.");
                return false;
            }
        } catch (Exception e) {
            log.error("Google Access Token validation failed", e);
            return false;
        }
    }

    public boolean isGoogleAccessTokenExpired(User user) {
        // google_access_token_expires_at이 유효한지 확인
        if (user.getGoogleAccessTokenExpiresAt() != null) {
            LocalDateTime expirationDate = user.getGoogleAccessTokenExpiresAt();
            // 현재 시간과 비교
            return expirationDate.isBefore(LocalDateTime.now());
        }
        return true; // 만약 만료시간이 없다면 만료된 것으로 간주
    }

    // 토큰 만료 시 갱신 로직
    public String refreshGoogleAccessToken(User user) {
        // 구글 리프레시 토큰으로 새로운 액세스 토큰 발급 받기
        try {
            String url = "https://oauth2.googleapis.com/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", "YOUR_GOOGLE_CLIENT_ID");
            params.add("client_secret", "YOUR_GOOGLE_CLIENT_SECRET");
            params.add("refresh_token", user.getGoogleRefreshToken());
            params.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, String> responseBody = response.getBody();
                String newAccessToken = responseBody.get("access_token");
                String newRefreshToken = responseBody.get("refresh_token");

                // 새 액세스 토큰과 리프레시 토큰 저장
                user.updateGoogleTokens(newAccessToken, newRefreshToken, LocalDateTime.now().plusHours(1), "new-jwt-token");
                return newAccessToken;
            } else {
                log.error("Failed to refresh Google Access Token.");
                return null;
            }
        } catch (Exception e) {
            log.error("Error refreshing Google Access Token", e);
            return null;
        }
    }


}
