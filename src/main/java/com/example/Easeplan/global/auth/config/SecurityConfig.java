package com.example.Easeplan.global.auth.config;

import com.example.Easeplan.global.auth.service.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @PostConstruct
    public void setSecurityContextHolderStrategy() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**","/auth/google/callback","/auth/google/events","/auth/google/free-time","/error",
                                "/error/**","/api/naver/**","/api/culture/events","/api/tour/location","/auth/google/eventsPlus",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**","/favicon.ico",
                                "/webjars/**","/api/survey","/api/devices/data","/api/fcm/send").permitAll()
                        .requestMatchers("/api/survey/select","/api/survey/scenarios","/short-recommend/**","/api/haru/**","/api/fcm/**","/api/mypage/**","/api/devices/smartwatch","/api/fcm/register").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
