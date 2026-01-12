package com.project.backend.global.config;

import com.project.backend.global.security.csrf.repository.CustomCookieCsrfTokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsrfConfig {

    @Bean
    public CustomCookieCsrfTokenRepository customCookieCsrfTokenRepository() {
        return new CustomCookieCsrfTokenRepository();
    }
}
