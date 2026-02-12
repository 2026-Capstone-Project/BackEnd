package com.project.backend.domain.auth.controller;

import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthException;
import com.project.backend.domain.auth.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth 소셜 로그인 통합 컨트롤러
 * GET /api/v1/auth/{provider} - 로그인 페이지 리다이렉트
 * GET /api/v1/auth/{provider}/callback - 콜백 처리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class OAuthController implements OAuthDocs {

    private final OAuthService oAuthService;

    // TODO: 프론트 배포 후 설정 파일로 분리
    private static final String FRONTEND_BASE_URL = "http://localhost:5173";
    private static final String FRONTEND_LOGIN_SUCCESS_URL = FRONTEND_BASE_URL + "/";
    private static final String FRONTEND_LOGIN_ERROR_URL = FRONTEND_BASE_URL + "/login";

    @Override
    @GetMapping("/{provider}")
    public void redirectToProvider(
            @PathVariable("provider") Provider provider,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException {
        oAuthService.redirectToProvider(provider, response, session);
    }

    @Override
    @GetMapping("/{provider}/callback")
    public void handleCallback(
            @PathVariable("provider") Provider provider,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) throws IOException {
        try {
            oAuthService.handleCallback(provider, code, state, request, response, session);

            // CORB 방지용 헤더 설정
            response.setContentType("text/html;charset=UTF-8");
            response.sendRedirect(FRONTEND_LOGIN_SUCCESS_URL);

        } catch (AuthException e) {
            log.warn("OAuth 로그인 실패 - provider: {}, errorCode: {}", provider, e.getCode().getCode());

            // 프론트엔드 에러 페이지로 리다이렉트 (쿼리 파라미터로 에러코드 전달)
            String errorRedirectUrl = UriComponentsBuilder.fromUriString(FRONTEND_LOGIN_ERROR_URL)
                    .queryParam("error", e.getCode().getCode())
                    .queryParam("message", e.getCode().getMessage())
                    .encode()
                    .build()
                    .toUriString();

            response.setContentType("text/html;charset=UTF-8");
            response.sendRedirect(errorRedirectUrl);
        }
    }
}
