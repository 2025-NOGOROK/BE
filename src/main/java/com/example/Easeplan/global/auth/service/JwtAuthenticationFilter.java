package com.example.Easeplan.global.auth.service;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.dto.JwtUtil;
import com.example.Easeplan.global.auth.repository.UserRepository;
import com.example.Easeplan.api.Calendar.service.GoogleOAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final GoogleOAuthService oAuthService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, GoogleOAuthService oAuthService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.oAuthService = oAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Swagger 관련 요청은 필터 적용하지 않음
        if (path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/error") ||
                path.startsWith("/swagger-resources")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7); // "Bearer "를 제외한 토큰 부분

                // JWT 토큰 검증 처리
                if (jwtUtil.isValidAccessToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    // 사용자 인증 정보 설정
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext에 인증 객체 설정
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("인증 실패: ", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 실패: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response); // 필터 체인을 계속 진행
    }


}
