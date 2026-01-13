package com.project.backend.global.security.csrf.repository;

import com.project.backend.global.security.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomCookieCsrfTokenRepository implements CsrfTokenRepository {

    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String CSRF_PARAMETER_NAME = "_csrf";
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

    private final CookieUtil cookieUtil;

    @Override
    @NonNull
    public CsrfToken generateToken(@NonNull HttpServletRequest request) {
        String token = UUID.randomUUID().toString();
        return new DefaultCsrfToken(CSRF_HEADER_NAME, CSRF_PARAMETER_NAME, token);
    }

    @Override
    public void saveToken(CsrfToken csrfToken, @NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        if (csrfToken == null) {
            return;
        }

        cookieUtil.createCsrfCookie(response, CSRF_COOKIE_NAME, csrfToken);
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
        cookieUtil.deleteCsrfCookie(response);
    }
}
