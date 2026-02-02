package com.project.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Bean("customCorsConfigurationSource")
    @Primary
    public CorsConfigurationSource apiConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:8080",
                "http://52.79.121.109:8080",
                "https://api.calio.kr"
        ));
        configuration.setAllowCredentials(true);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("OPTIONS", "GET", "POST", "PATCH", "DELETE"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명(쿠키) 허용
        configuration.setAllowCredentials(true);

        // 프론트가 읽을 수 있는 헤더
        configuration.setExposedHeaders(List.of(
                "X-XSRF-TOKEN"      // CSRF 토큰
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
