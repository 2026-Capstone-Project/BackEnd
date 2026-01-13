package com.project.backend.global.security.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CookieUtil {

    private final boolean secure;
    private final String sameSite;

    public CookieUtil(
            @Value("${spring.cookie.secure:true}") boolean secure,
            @Value("${spring.cookie.same-site:None}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /**
     * JWT 쿠키 생성
     */
    public void createJwtCookie(HttpServletResponse response, String name, String token, long tokenExpMs) {
        Cookie jwtCookie = new Cookie(name, token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(secure);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (tokenExpMs / 1000));
        jwtCookie.setAttribute("SameSite", sameSite);
        response.addCookie(jwtCookie);
    }

    /**
     * CSRF 쿠키 생성
     */
    public void createCsrfCookie(HttpServletResponse response, String name, CsrfToken csrfToken) {
        Cookie csrfCookie = new Cookie(name, csrfToken.getToken());
        csrfCookie.setHttpOnly(false);
        csrfCookie.setSecure(secure);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(-1);
        csrfCookie.setAttribute("SameSite", sameSite);
        response.addCookie(csrfCookie);
    }

    /**
     * 쿠키 삭제
     */
    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", sameSite);
        response.addCookie(cookie);
    }

    /**
     * CSRF 쿠키 삭제
     */
    public void deleteCsrfCookie(HttpServletResponse response) {
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", null);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(0);
        response.addCookie(csrfCookie);
    }

    /**
     * 쿠키에서 토큰 추출
     */
    public String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        log.debug("[ CookieUtil ] 쿠키 검색: {}", cookieName);

        if (request.getCookies() == null) {
            log.debug("[ CookieUtil ] 쿠키가 존재하지 않음");
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                log.debug("[ CookieUtil ] {} 쿠키 존재", cookieName);
                return cookie.getValue();
            }
        }

        log.debug("[ CookieUtil ] {} 쿠키가 존재하지 않음", cookieName);
        return null;
    }
}
