package com.project.backend.global.security.csrf.repository;



import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class CustomCookieCsrfTokenRepository implements CsrfTokenRepository {

    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAMETER_NAME = "_csrf";
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";


    @Override
    @NonNull
    public CsrfToken generateToken(@NonNull HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, token);
    }

    @Override
    public void saveToken(CsrfToken csrfToken, @NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
//        if (response == null) return;

        if (csrfToken == null) {
            // 여기서 바로 삭제하지 않음 (로그아웃 시 직접 전용 메서드 호출로 삭제)
            return;
        }

        createCsrfCookies(response, CSRF_COOKIE_NAME, csrfToken);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CSRF_COOKIE_NAME.equals(c.getName()) && StringUtils.hasText(c.getValue())) {
                    return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, c.getValue());
                }
            }
        }
        return null;
    }

    public void invalidateCsrfToken(HttpServletResponse response) {
        Cookie csrfCookie = new Cookie(CSRF_COOKIE_NAME, null);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(0);
        response.addCookie(csrfCookie);
    }

    private void createCsrfCookies(HttpServletResponse response, String name, CsrfToken csrfToken) {
        Cookie csrfCookie = new Cookie(name, csrfToken.getToken());
        // csrf 쿠키는 js가 읽어야 헤더에 넣을 수 있음
        csrfCookie.setHttpOnly(false);
        // HTTPS 연결에서만 쿠키 전송
        csrfCookie.setSecure(true);
        // '/' 경로 이하 모든 API 요청에 쿠키가 포함되도록
        csrfCookie.setPath("/");
        // 우리 도메인에서만 사용
//        csrfCookie.setDomain("calio.com");
        // -1 세션이 종료하면 쿠키 삭제
        csrfCookie.setMaxAge(-1);
        // CSRF 설정 -> 배포 중에는 Lax
        csrfCookie.setAttribute("SameSite", "None");
        // 쿠키 추가
        response.addCookie(csrfCookie);
    }

}
