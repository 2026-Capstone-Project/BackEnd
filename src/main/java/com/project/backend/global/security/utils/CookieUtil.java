package com.project.backend.global.security.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private final boolean secure;
    private final String sameSite;

    public CookieUtil(
            @Value("${spring.cookie.secure}") boolean secure,
            @Value("${spring.cookie.same-site}") String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /**
     * 쿠키 생성
     */
    public void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            long maxAgeMs
    ) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (maxAgeMs / 1000));
        cookie.setSecure(secure);
        cookie.setAttribute("SameSite", sameSite);

        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제
     */
    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(secure);
        cookie.setAttribute("SameSite", sameSite);

        response.addCookie(cookie);
    }

    /**
     * 쿠키에서 값 추출
     */
    public String extractFromCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}