package com.project.backend.global.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;

@Slf4j
public class CookieUtils {
    // 쿠키를 생성하는 메서드
    public static void createJwtCookies(HttpServletResponse response, String name, String token, long tokenExpMs) {
        // jwtDto 에서 access token을 꺼내서 accessToken이라는 이름의 쿠키 생성
        Cookie jwtCookie = new Cookie(name, token);
        // JS 에서 쿠키 읽기 불가능 XSS 방지
        jwtCookie.setHttpOnly(true);
        // HTTPS 연결에서만 쿠키 전송
        jwtCookie.setSecure(true);
        // '/' 경로 이하 모든 API 요청에 쿠키가 포함되도록
        jwtCookie.setPath("/");
        // 우리 도메인에서만 사용
//        jwtCookie.setDomain("calio.com");
        // 쿠키 만료 시간 환경변수로 받아옴 (MS -> Sec로 변환 하려고 /1000)
        jwtCookie.setMaxAge((int) (tokenExpMs / 1000));
        // CSRF 설정 -> 개발 중에는 None
        jwtCookie.setAttribute("SameSite", "None");
        // 쿠키 추가
        response.addCookie(jwtCookie);
    }

    public static void createCsrfCookies(HttpServletResponse response, String name, CsrfToken csrfToken) {
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

    public static void deleteCsrfCookie(HttpServletResponse response) {
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", null);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(0);
        response.addCookie(csrfCookie);
    }


    public static String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        log.debug("[ CookieUtils ] 쿠키 검색");
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    log.debug("[ CookieUtils ] {} 쿠키 존재", cookieName);
                    return cookie.getValue();
                }
            }

            log.warn("[ CookieUtils ] 사용 가능한 쿠키가 존재하지 않음");
            log.debug("[ CookieUtils ] 현재 쿠키 목록 -------------------");
            for (Cookie cookie : request.getCookies()) {
                log.debug("[ CookieUtils ]  - {}", cookie.getName());
                log.debug("[ CookieUtils ] ------------------------------------");
            }
            log.debug("[ CookieUtils ] {} 쿠키가 존재하지 않음", cookieName);
        }
        return null;
    }
}
