package com.project.backend.global.security.controller;

import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.service.SecurityService;
import com.project.backend.global.security.utils.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/security")
public class SecurityController {

    private final CookieUtil cookieUtil;
    private final SecurityService securityService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "엑세스 쿠키 재발급", description = "엑세스 쿠키가 만료되어 없어졌고, 리프레시 쿠키가 있다면 엑세스 쿠키를 만들어준다.")
    @PostMapping("/reissue-cookie")
    public CustomResponse<String> reissueCookie(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieUtil.getTokenFromCookie(request, "refresh-token");
        String accessToken = securityService.reissueCookie(refreshToken);

        // 쿠키 재발급
        log.debug("[ JwtAuthorizationFilter ] 쿠키를 재생성 합니다.");
        cookieUtil.createJwtCookie(response, "access-token", accessToken, jwtUtil.getAccessExpMs());

        return CustomResponse.onSuccess("OK", "엑세스 쿠키가 재발급 되었습니다.");
    }

    @Operation(summary = "CSRF 토큰 발급", description = "CSRF 토큰을 쿠키로 발급합니다")
    @GetMapping("/csrf")
    public CustomResponse<String> csrf(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // lazy 토큰을 실제로 생성해서 쿠키에 담기도록 트리거
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken(); // 호출 시 쿠키가 Set-Cookie 로 내려감
        }

        return CustomResponse.onSuccess("OK", "CSRF 토큰이 쿠키로 발급되었습니다.");
    }
}
