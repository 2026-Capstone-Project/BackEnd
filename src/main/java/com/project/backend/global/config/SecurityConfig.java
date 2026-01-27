package com.project.backend.global.config;

import com.project.backend.global.security.exception.CustomAccessDeniedHandler;
import com.project.backend.global.security.exception.CustomAuthenticationEntryPoint;
import com.project.backend.global.security.csrf.repository.CustomCookieCsrfTokenRepository;
import com.project.backend.global.security.handler.CustomLogoutHandler;
import com.project.backend.global.security.jwt.JwtAuthorizationFilter;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.utils.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisUtils;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomCookieCsrfTokenRepository customCookieCsrfTokenRepository;
    private final CustomLogoutHandler customLogoutHandler;
    private final CookieUtil cookieUtil;
    private final @Qualifier("customCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource;

    private final String[] allowUrl = {
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/api/v1/auth/**",
            "/api/v1/security/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        http
                // CORS CONFIG
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 접근 설정
                .authorizeHttpRequests(req -> req
                        // 허용 url 목록은 모두 허용
                        .requestMatchers(allowUrl).permitAll()
                        // 그 외 모든 요처에 대해서 인증
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 등록
                .addFilterBefore(new JwtAuthorizationFilter(jwtUtil, redisUtils, cookieUtil), UsernamePasswordAuthenticationFilter.class)

                // SPRING SECURITY 기본 로그인 폼 비활성화 -> REST API 기반 JWT 사용
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP BASIC 인증 비활성화 -> JWT은 사용하지 않음
                .httpBasic(HttpBasicConfigurer::disable)

//                // JSESSIONID 비활성화
//                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//                // CSRF 설정 비활성화
////                .csrf(AbstractHttpConfigurer::disable)
//                // 일단 비활성화
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(customCookieCsrfTokenRepository)
//                        // 로그인/회원가입/문서 등 최소 범위만 예외. 이후 프론트가 헤더 붙이면 예외 줄여도 됨.
//                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
//                        .ignoringRequestMatchers(
//                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**",
//                                "/actuator/**"
//                        )
//                )
                // ✅ 완전 Stateless
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ✅ CSRF 완전 비활성화
                .csrf(AbstractHttpConfigurer::disable)


                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                        })
                )

                // 예외 처리 핸들러 설정
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // 인증 자체가 안 된 경우 (401)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        // 인증은 되었지만 권한이 없을 때 (403)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
        ;

        return http.build();
    }
}
