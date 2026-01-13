package com.project.backend.domain.auth.controller;

import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.service.OAuthService;
import com.project.backend.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * OAuth 소셜 로그인 통합 컨트롤러
 * GET /api/v1/auth/{provider} - 로그인 페이지 리다이렉트
 * GET /api/v1/auth/{provider}/callback - 콜백 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OAuthController implements OAuthDocs {

    private final OAuthService oAuthService;

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
    public CustomResponse<String> handleCallback(
            @PathVariable("provider") Provider provider,
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        oAuthService.handleCallback(provider, code, state, request, response, session);
        return CustomResponse.onSuccess("OK", provider.name() + " 로그인 성공");
    }
}
