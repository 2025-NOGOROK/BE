package com.example.Easeplan.global.auth.service;

import com.example.Easeplan.global.auth.domain.User;
import com.example.Easeplan.global.auth.dto.JwtUtil;
import com.example.Easeplan.global.auth.repository.UserRepository;
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

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
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
                path.startsWith("/swagger-resources")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                logger.info("JWT 토큰 파싱 시도: " + token); // ✅ 로깅 추가

                if (jwtUtil.isValidAccessToken(token)) {
                    String email = jwtUtil.getEmailFromToken(token);
                    logger.info("토큰 유효 - 이메일: " + email); // ✅ 로깅 추가

                    userRepository.findByEmail(email).ifPresent(user -> {
                        logger.info("사용자 조회 성공: " + user.getEmail()); // ✅ 로깅 추가
                        // UserDetails로 변환 (User가 UserDetails를 구현했으므로 직접 사용 가능)
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user, // UserDetails 구현체
                                        null,
                                        user.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.info("SecurityContext에 인증 객체 설정 완료"); // ✅ 로깅 추가
                    });
                }
                else {
                    logger.error("토큰 유효성 검사 실패"); // ✅ 로깅 추가
                }
            }
        } catch (Exception e) {
            logger.error("JWT 인증 오류: ", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 실패: " + e.getMessage());
            return; // ✅ 예외 발생 시 필터 체인 중단
        }

        filterChain.doFilter(request, response);
    }
}
