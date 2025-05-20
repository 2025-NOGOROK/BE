package com.example.Easeplan.global.auth.config;

import com.example.Easeplan.global.auth.service.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment env;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, Environment env) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.env = env;
    }

    @PostConstruct
    public void setSecurityContextHolderStrategy() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ⭐️ 개선된 CORS 설정 ⭐️
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 운영 환경이면 특정 도메인만 허용, 개발 환경이면 모든 도메인 허용
        boolean isProd = isProdEnvironment();
        if (isProd) {
            config.setAllowedOrigins(List.of(
                    "https://recommend.ai.kr",
                    "https://api.recommend.ai.kr"
            ));
        } else {
            config.addAllowedOriginPattern("*");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private boolean isProdEnvironment() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod") ||
                "prod".equals(activeProfile);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 운영 환경에서만 HTTPS 강제
        if (isProdEnvironment()) {
            http.requiresChannel(channel ->
                    channel.anyRequest().requiresSecure());
        }

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error","/callback.html",
                                "/static/**",
                                "/error/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**","/api/devices/data","/api/fcm/send",
                                "/",                // 루트 경로 추가
                                "/favicon.ico",     // favicon도 permitAll에 포함
                                "/auth/**",
                                "/auth/google/callback",
                                "/auth/google/events",
                                "/auth/google/free-time",
                                "/api/culture/events",
                                "/api/tour/location",
                                "/auth/google/eventsPlus",
                                "/api/survey").permitAll()
                        .requestMatchers("/api/survey/select","/api/survey/scenarios","/short-recommend/**","/api/haru/**","/api/fcm/**","/api/mypage/**","/api/devices/smartwatch","/api/fcm/register").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
