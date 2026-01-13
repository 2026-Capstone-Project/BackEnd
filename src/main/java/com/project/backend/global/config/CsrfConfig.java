package com.project.backend.global.config;

import com.project.backend.global.security.csrf.repository.CustomCookieCsrfTokenRepository;
import com.project.backend.global.security.utils.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CsrfConfig {

    private final CookieUtil cookieUtil;

    @Bean
    public CustomCookieCsrfTokenRepository customCookieCsrfTokenRepository() {
        return new CustomCookieCsrfTokenRepository(cookieUtil);
    }
}
