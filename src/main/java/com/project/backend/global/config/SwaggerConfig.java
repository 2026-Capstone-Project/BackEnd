package com.project.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger() {
        Info info = new Info().title("2026 Capstone Project").description("2026 Capstone Project API").version("0.0.1");

        String csrfSchemeName = "X-XSRF-TOKEN";
        String csrfCookieName = "XSRF-TOKEN";
        // API 요청헤더에 인증정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(csrfSchemeName).addList(csrfCookieName);

        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(csrfSchemeName, new SecurityScheme()
                        .name(csrfSchemeName)
                        .type(SecurityScheme.Type.APIKEY) // csrf token은 api key 방식
                        .in(SecurityScheme.In.HEADER) // header에 위치
                        .description("csrf token을 헤더에 입력하세요"));

        components.addSecuritySchemes(csrfCookieName,
                new SecurityScheme()
                        .name(csrfCookieName)
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .description("csrf token 쿠키 입력"));

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
